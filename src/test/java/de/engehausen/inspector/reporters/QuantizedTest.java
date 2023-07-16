package de.engehausen.inspector.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.engehausen.inspector.data.ClassInfo;
import de.engehausen.inspector.data.Report;
import de.engehausen.inspector.reporters.AbstractWeightMapper.Weight;

class QuantizedTest {

	@Test
	void testPercentiles() {
		var report = new Report(
			Map.of(
				FileCorrelatorTest.class.getName(), new ClassInfo(75, Collections.emptyMap()),
				PercentileTest.class.getName(), new ClassInfo(25, Collections.emptyMap()),
				ThresholdTest.class.getName(), new ClassInfo(1, Collections.emptyMap())
			),
			Collections.emptyMap()
		);
		final List<Weight> result = new Quantized().transform(report, Map.of(FileCorrelator.KEY_SOURCE_ROOTS, List.of(System.getProperty("user.dir"))));
		final List<Weight> expected = List.of(
			new Weight("src/test/java/de/engehausen/inspector/reporters/ThresholdTest.java", 0.25d),
			new Weight("src/test/java/de/engehausen/inspector/reporters/PercentileTest.java", 0.75d),
			new Weight("src/test/java/de/engehausen/inspector/reporters/FileCorrelatorTest.java", 1d)
		);
		Assertions.assertEquals(expected, result);
	}

}
