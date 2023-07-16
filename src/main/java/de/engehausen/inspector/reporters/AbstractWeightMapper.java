package de.engehausen.inspector.reporters;

import java.util.List;
import java.util.Map;

import de.engehausen.inspector.data.Report;
import de.engehausen.inspector.data.Reporter;

/**
 * Maps the classes of a report to weights and returns a list of these.
 */
public abstract class AbstractWeightMapper implements Reporter<List<AbstractWeightMapper.Weight>> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Weight> transform(final Report report, final Map<String, Object> meta) {
		return report(new FileCorrelator()
			.transform(report, meta)
			.classes()
			.entrySet()
			.stream()
			.map(entry -> new Weight(entry.getKey(), Integer.valueOf(entry.getValue().totalCalls())))
			.sorted((a, b) -> a.weight().intValue() - b.weight().intValue())
			.toList(),
			meta
		);
	}

	/**
	 * Returns the list of weights to report.
	 * @param weights the sorted input weights, which are the number of calls per class
	 * @param meta configuration information
	 * @return a list of weights
	 */
	public abstract List<Weight> report(final List<Weight> weights, final Map<String, Object> meta);

	/**
	 * An entry of the weight list.
	 * @param name the source file name
	 * @param weight the weight of the class identified by the name
	 */
	public record Weight(String name, Number weight) {};
}
