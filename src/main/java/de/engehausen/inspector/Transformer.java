package de.engehausen.inspector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Configuration;
import de.engehausen.inspector.data.Report;
import de.engehausen.inspector.data.Reporter;
import de.engehausen.inspector.reporters.Identity;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

/**
 * Transformer which tracks methods calls and outputs
 * class and method usage (configurable).
 */
public class Transformer implements ClassFileTransformer {

	protected final Map<String, Map<String, AtomicInteger>> classesUsed;
	protected final Set<ClassLoader> loadersUsed;
	protected final ClassPool classPool;
	protected final List<String> issues;

	protected final Pattern excludes;
	protected final Pattern includes;
	protected final OutputStream out;
	protected final boolean reportIssues;
	protected final boolean details; 
	protected final Reporter<?> reporter;
	protected final Map<String, Object> meta;

	private static Transformer INSTANCE;

	/**
	 * Creates the transformer.
	 * 
	 * @param agentArgs the agent arguments as specified on the command line for
	 * {@code -javaagent}. This can be <em>either</em> {@link Configuration#ARG_CONFIG}
	 * to point to a JSON file with the configuration, or individual other {@code ARG_...}
	 * arguments specified as a key value pair separated by {@code =}. Key value pairs
	 * must be separated by {@code :}. Example configurations: a) {@code -javaagent:..jar=config=myjconfig.json},
	 * b) {@code -javaagent:..jar=excludes=com+,org+:out=/tmp/result.json}.
	 */
	protected Transformer(final String agentArgs) {
		// since the instrumented classes are determine here once,
		// these to collections are not created thread-safe
		classesUsed = new HashMap<>();
		loadersUsed = new HashSet<>();
		// this will be accessed only with synchronization
		issues = new ArrayList<>();
		classPool = ClassPool.getDefault();
		classPool.childFirstLookup = true;
		final Map<String, String> args = Stream
			.of((agentArgs != null ? agentArgs : "").split(":"))
			.filter(str -> str != null && str.indexOf('=') > 0)
			.map(keyValue -> keyValue.split("="))
			.collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));
		final Configuration configuration = Optional
			.ofNullable(args.get(Configuration.ARG_CONFIG))
			.map(fileName -> {
				try {
					return new ObjectMapper().readValue(new File(fileName), Configuration.class);
				} catch (IOException e) {
					System.err.println(e.getMessage());
					System.exit(1);
				}
				return null;
			}).or(() -> Optional.of(new Configuration(
				toList(args.get(Configuration.ARG_EXCLUDES)),
				toList(args.get(Configuration.ARG_EXCLUDES)),
				Boolean.parseBoolean(args.getOrDefault(Configuration.ARG_DETAILS, Boolean.TRUE.toString())),
				args.get(Configuration.ARG_OUT),
				args.get(Configuration.ARG_REPORT_ISSUES),
				args.get(Configuration.ARG_REPORTER),
				toMap(args.get(Configuration.ARG_META)))
			))
			.get();
		excludes = getPattern(configuration.excludes(), "^$");
		includes = getPattern(configuration.includes(), ".+");
		details = configuration.details();
		out = Optional
			.ofNullable(configuration.out())
			.map(fileName -> {
				try {
					return (OutputStream) new FileOutputStream(fileName);
				} catch (FileNotFoundException e) {
					return (OutputStream) System.err;
				}
			})
			.orElse(System.err);
		reportIssues = Optional
			.ofNullable(configuration.reportIssues())
			.map(Boolean::parseBoolean)
			.orElse(Boolean.TRUE)
			.booleanValue();
		meta = configuration.meta();
		final String reporterName = Optional
			.ofNullable(configuration.reporter())
			.orElse(Identity.NAME);
		reporter = ServiceLoader
			.load(Reporter.class)
			.stream()
			.map(Provider::get)
			.filter(Objects::nonNull)
			.filter(candidate -> reporterName.equals(candidate.name()))
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("cannot find reporter '%s'".formatted(reporterName)));
		INSTANCE = this;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized byte[] transform(
		final Module module,
		final ClassLoader loader,
		final String className,
		final Class<?> classBeingRedefined,
		final ProtectionDomain protectionDomain,
		final byte[] classfileBuffer) throws IllegalClassFormatException {
		if (untransformable(className) || reject(className)) {
			return classfileBuffer;
		}
		var counters = new ConcurrentHashMap<String, AtomicInteger>();
		if (classesUsed.put(className, counters) != null) {
			recordIssue("duplicated class %s".formatted(className));
		}
		if (loadersUsed.add(loader)) {
			classPool.insertClassPath(new LoaderClassPath(loader));
		}
		return monitorMethods(className, counters, classfileBuffer);
	}

	// counts method invocations
	public static void count(final String className, final String method) {
		Optional
			.ofNullable(instance().classesUsed.get(className))
			.ifPresent(counters -> 
				Optional
					.ofNullable(counters.get(method))
					.ifPresent(counter -> counter.incrementAndGet())
			);
	}

	/**
	 * Shutdown hook to produce the {@link #report()}.
	 * @return a thread with the reporter.
	 */
	public Thread atShutdown() {
		return new Thread(this::report);
	}

	/**
	 * Returns the singleton transformer instance.
	 * @return the singleton transformer instance.
	 */
	protected static Transformer instance() {
		return INSTANCE;
	}

	/**
	 * Returns a regular expression pattern based on a list of
	 * regular expressions.
	 * @param regexs a list of regular expressions, may be {@code null}
	 * @param defaultPattern the default pattern to use of no expressions are given in the first parameter
	 * @return a pattern to be used for exclusions and inclusions
	 */
	protected Pattern getPattern(final List<String> regexs, final String defaultPattern) {
		if (regexs == null || regexs.isEmpty()) {
			return Pattern.compile(defaultPattern, Pattern.MULTILINE);
		}
		return Pattern.compile(regexs
			.stream()
			.reduce("", (current, next) -> {
				if (current.length() == 0) {
					return "(%s)".formatted(next);
				}
				return "%s|(%s)".formatted(current, next);
			})
		);
	}

	/**
	 * Attempts to instrument the given class to count its method invocations.
	 * Some standard packages (JDK etc) are always excluded.
	 * 
	 * @param className the name of the class to instrument
	 * @param counters a thread-safe map of counter for method names of the class being instrumented
	 * @param classfileBuffer the classfile contents
	 * @return the potentially modified classfile
	 */
	protected byte[] monitorMethods(final String className, final Map<String, AtomicInteger> counters, final byte[] classfileBuffer) {
		final String name = Descriptor.toJavaName(className);

		try {
			final CtClass srcClass = classPool.get(name);
			if (modifiable(srcClass)) {
				final CtMethod[] methods = srcClass.getDeclaredMethods();
				for (final CtMethod method : methods) {
					if (!Modifier.isAbstract(method.getModifiers())) {
						final String methodName = method.getName() + method.getSignature();
						counters.put(methodName, new AtomicInteger());
						final String countCode = "{ de.engehausen.inspector.Transformer.count(\"" + className + "\",\"" + methodName + "\"); } ";
						try {
							method.insertBefore(countCode);
						} catch (Throwable t) {
							recordIssue("cannot insert counter to %s%s: %s=%s".formatted(name, methodName, t.getClass().getName(), t.getMessage()));
							return classfileBuffer;
						}
					}
				}
				try {
					return srcClass.toBytecode();
				} finally {
					srcClass.detach();
				}
			}
		} catch (CannotCompileException|NotFoundException|IOException e) {
			recordIssue("cannot instrument %s: %s=%s".formatted(name, e.getClass().getName(), e.getMessage()));
		}
		return classfileBuffer;
	}

	/**
	 * Classes that are generally excluded from instrumentation.
	 * @param className the name of the class to check
	 * @return {@code true} if the class is not to be instrumented
	 */
	protected boolean untransformable(final String className) {
		return className.startsWith("java/") || 
			className.startsWith("com/sun/") ||
			className.startsWith("sun/") ||
			className.startsWith("jdk/") ||
			className.startsWith("de/engehausen/inspector");
	}

	/**
	 * Checks the name of the class with the exclusion and inclusion patterns.
	 * Classes that match the exclusions or don't match the inclusions are
	 * ignored
	 * @param className the name of the class to check
	 * @return  {@code true} if the class is to be ignored
	 */
	protected boolean reject(final String className) {
		return excludes.matcher(className).matches() ||
			!includes.matcher(className).matches();
	}

	/**
	 * Checks whether the given class can be instrumented.
	 * @param candidate the class to check
	 * @return {@code true} if it is considered instrumentable.
	 */
	protected boolean modifiable(final CtClass candidate) {
		return !(candidate.isAnnotation() || candidate.isArray() || candidate.isPrimitive());
	}

	/**
	 * Records an issue that occurred during instrumentation.
	 * @param message the message to record
	 */
	protected void recordIssue(final String message) {
		synchronized (issues) {
			issues.add(message);
		}
	}

	/**
	 * Outputs the JSON report, either to {@code System.err} (default) or to a file.
	 */
	protected void report() {
		if (reportIssues) {
			System.err.println("Issues seen: " + reportIssues);
			synchronized (issues) {
				issues.forEach(System.err::println);
			}
		}
		final var report = new Report(
			classesUsed
				.entrySet()
				.stream()
				.map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), new ClassInfo(
					entry
						.getValue()
						.values()
						.stream()
						.mapToInt(AtomicInteger::get)
						.sum(),
					details ? entry.getValue() : null)))
				.filter(entry -> entry.getValue().totalCalls() > 0)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
				null
		);
		try {
			new ObjectMapper().writeValue(out, reporter.transform(report, meta));
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Converts a comma-separated list to a regular list of strings.
	 * @param csv the input
	 * @return the result list
	 */
	private List<String> toList(final String csv) {
		return csv != null ? Stream.of(csv.split(",")).toList() : Collections.emptyList();
	}

	/**
	 * Reads meta information in JSON format from a file
	 * @param fileName the file to read
	 * @return the meta information
	 */
	private Map<String, Object> toMap(final String fileName) {
		if (fileName != null) {
			try {
				return new ObjectMapper().readValue(new File(fileName), new TypeReference<Map<String, Object>>() {});
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
		return Collections.emptyMap();
	}

}
