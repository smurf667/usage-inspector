package de.engehausen.inspector.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Report;

class FileCorrelatorTest {

	@Test
	void testCorrelation() {
		final String unknown = "a.b.c.HelloWorld";
		var info = new ClassInfo(0, Collections.emptyMap());
		var report = new Report(
			Map.of(
				FileCorrelatorTest.class.getName(), info,
				unknown, info
			),
			Collections.emptyMap()
		);
		final Report result = new FileCorrelator().transform(report, Map.of(FileCorrelator.KEY_SOURCE_ROOTS, List.of(System.getProperty("user.dir"))));
		Assertions.assertEquals(1, result.classes().size());
		final Map<String, Object> meta = result.meta();
		Assertions.assertNotNull(meta);
		final Object problems = meta.get(FileCorrelator.KEY_NOT_FOUND);
		Assertions.assertTrue(problems instanceof List<?>);
		final List<?> notFound = (List<?>) problems;
		Assertions.assertTrue(notFound.contains(unknown));
	}

}
