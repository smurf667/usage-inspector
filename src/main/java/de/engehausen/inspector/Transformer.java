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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Configuration;
import de.engehausen.inspector.data.Report;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;

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
		classesUsed = new HashMap<>();
		loadersUsed = new HashSet<>();
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
				args.get(Configuration.ARG_REPORT_ISSUES))
				)
			)
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
		INSTANCE = this;
	}

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

	public Thread atShutdown() {
		return new Thread(this::report);
	}

	protected static Transformer instance() {
		return INSTANCE;
	}

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

	protected boolean untransformable(final String className) {
		return className.startsWith("java/") || 
			className.startsWith("com/sun/") ||
			className.startsWith("sun/") ||
			className.startsWith("jdk/") ||
			className.startsWith("de/engehausen/inspector");
	}

	protected boolean reject(final String className) {
		// hm, think this through ;-)
		return excludes.matcher(className).matches() ||
			!includes.matcher(className).matches();
	}

	protected boolean modifiable(final CtClass candidate) {
		return !(candidate.isAnnotation() || candidate.isArray() || candidate.isPrimitive());
	}

	protected void recordIssue(final String message) {
		synchronized (issues) {
			issues.add(message);
		}
	}

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
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
		);
		try {
			new ObjectMapper().writeValue(out, report);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
	}

	private List<String> toList(final String csv) {
		return csv != null ? Stream.of(csv.split(",")).toList() : Collections.emptyList();
	}

}
