package de.engehausen.inspector.reporters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Report;
import de.engehausen.inspector.data.Reporter;

/**
 * <p>Attempts to map class names to source files.
 * This scans the file system for source files. It uses multiple
 * source roots and extensions. Any class that cannot be
 * correlated to its source file is dropped from the report.</p>
 * <p>This reporter requires {@link FileCorrelator#KEY_SOURCE_ROOTS}
 * in the additional meta configuration.</p>
 */
public class FileCorrelator implements Reporter<Report> {

	private static final String JAVA_EXTENSION = "java";

	/** {@code correlator} */
	public static final String NAME = "correlator";

	/** {@code sourceRoots} - a list of source root folders (mandatory) */
	public static final String KEY_SOURCE_ROOTS = "sourceRoots";
	/** {@code extensions} - a list of extensions (optional, defaults to {@code [ "java" ]} */
	public static final String KEY_EXTENSIONS = "extensions";
	/** {@code notFound} - optional output list of classes that could not be correlated */
	public static final String KEY_NOT_FOUND = "notFound";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String name() {
		return NAME;
	}

	/**
	 * Takes the classes of the report and tries to match each class to a
	 * known source file (scans file system and checks with a list of file name
	 * extensions).
	 * @param report the report.
	 * @param meta a map of additional configuration data.
	 * @return a new report
	 */
	@Override
	public Report transform(final Report report, final Map<String, Object> meta) {
		final List<String> extensions = extensions(meta.get(KEY_EXTENSIONS));
		final Set<String> sources = sourceFiles(meta.get(KEY_SOURCE_ROOTS), extensions);
		final Map<String, ClassInfo> next = new HashMap<>();
		final Map<String, Object> metaNext = new HashMap<String, Object>();
		Optional
			.ofNullable(report.meta())
			.ifPresent(metaNext::putAll);
		final List<String> notFound = new ArrayList<>();
		report.classes()
			.forEach((key, value) -> {
				extensions
					.stream()
					.map(extension -> {
						var suffix = "%s.%s".formatted(key, extension);
						return sources
							.stream()
							.filter(candidate -> candidate.endsWith(suffix))
							.findFirst()
							.orElse(null);
					})
					.filter(str -> str != null)
					.findFirst()
					.ifPresentOrElse(
						source -> next.put(source, value),
						() -> notFound.add(key)
					);
			});
		if (!notFound.isEmpty()) {
			metaNext.put(KEY_NOT_FOUND, notFound);
		}
		return new Report(next, metaNext.isEmpty() ? null : metaNext);
	}

	protected List<String> extensions(final Object info) {
		if (info instanceof List<?> list) {
			return list.stream().map(Object::toString).toList();
		}
		return List.of(JAVA_EXTENSION);
	}

	protected Set<String> sourceFiles(final Object in, final List<String> extensionList) {
		if (in instanceof List<?> roots) {
			final Set<String> extensions = new HashSet<>(extensionList);
			return roots
				.stream()
				.map(Object::toString)
				.flatMap(root -> files(root, extensions)) 
				.distinct()
				.collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	protected Stream<String> files(final String base, final Set<String> extensions) {
		final Path root = Paths.get(base);
		try {
			return Files
				.find(
					root,
					Integer.MAX_VALUE,
					(filePath, fileAttr) -> fileAttr.isRegularFile() && extensions.contains(extension(filePath))
				).map(path -> root.relativize(path))
				.map(path -> path.toString().replace(File.separatorChar, '/'));
		} catch (IOException e) {
			return Stream.empty();
		}
	}

	protected String extension(final Path filePath) {
		return Optional
			.ofNullable(filePath.getFileName())
			.map(Path::toString)
			.map(name -> {
				final int idx = name.lastIndexOf('.');
				return idx >= 0 ? name.substring(1 + idx) : "";
			}).orElse("");
	}
}
