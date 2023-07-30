package de.engehausen.inspector.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps the classes of the input report to a list of weights
 * according to the percentile of each class (0..1).
 */
public class Percentile extends AbstractWeightMapper {

	/** {@code percentile} */
	public static final String NAME = "percentile";

	private static final double DIV = 10000d;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String name() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Weight> report(final List<Weight> weights, final Map<String, Object> meta) {
		final List<Weight> result = new ArrayList<>(weights.size());
		final int max = weights.size();
		final double maxd = max;
		for (int index = 0; index < max; index++) {
			final Weight entry = weights.get(index);
			result.add(new Weight(
				entry.path(),
				Double.valueOf(Math.round(DIV * (1 + index) / maxd) / DIV))
			);
		}
		return result;
	}

}
