package de.engehausen.inspector;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Report;

class AgentTest {

	private static final String EXPECTED_CLASS = "de/engehausen/example/ApplicationDemo";
	private static final String EXPECTED_CLASS_SOURCE = "src/test/java/de/engehausen/example/ApplicationDemo.java";

	@Test
	void verifyStandardResults() throws IOException {
		final Report report = new ObjectMapper().readValue(new File("target/report.json"), Report.class);
		Assertions.assertNotNull(report, "report not found");
		final Map<String, ClassInfo> all = report.classes();
		Assertions.assertNotNull(all, "no classes recorded");
		final ClassInfo info = all.get(EXPECTED_CLASS);
		Assertions.assertNotNull(info, () -> "%s not recorded".formatted(EXPECTED_CLASS));
		Assertions.assertEquals(10, info.totalCalls(), "total call mismatch");
		final Map<String, AtomicInteger> calls = info.methodCalls();
		Assertions.assertNotNull(calls, "no calls recorded");
		Stream.of(
			new Entry("once()Z", 1),
			new Entry("twice()I", 2),
			new Entry("factorial(I)I", 5),
			new Entry("performRecursion()V", 1),
			new Entry("performCalls()V", 1)
		).forEach(entry -> {
			final AtomicInteger actual = calls.get(entry.key());
			Assertions.assertNotNull(actual, () -> "no entry found for %s".formatted(entry.key()));
			Assertions.assertEquals(entry.value(), actual.intValue(), () -> entry.key());
		});
		final Optional<String> illegal = all
			.keySet()
			.stream()
			.filter(str -> str.contains("javax") || str.contains("de/engehausen/ignored"))
			.findAny();
		Assertions.assertTrue(illegal.isEmpty(), "report must not contain excluded classes");
	}

	@Test
	void verifySourceCorrelator() throws IOException {
		final Report report = new ObjectMapper().readValue(new File("target/report-with-sources.json"), Report.class);
		Assertions.assertNotNull(report, "report not found");
		final Map<String, ClassInfo> all = report.classes();
		Assertions.assertNotNull(all, "no classes recorded");
		final ClassInfo info = all.get(EXPECTED_CLASS_SOURCE);
		Assertions.assertNotNull(info, () -> "%s not recorded".formatted(EXPECTED_CLASS_SOURCE));
	}

	private static record Entry(String key, int value) {};
}
