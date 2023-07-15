package de.engehausen.inspector.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Percentile extends AbstractWeightMapper {

	public static final String NAME = "percentile";

	private static final double DIV = 10000d;

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public List<Weight> report(final List<Weight> weights, final Map<String, Object> meta) {
		final List<Weight> result = new ArrayList<>(weights.size());
		final int max = weights.size();
		final double maxd = max;
		for (int index = 0; index < max; index++) {
			final Weight entry = weights.get(index);
			result.add(new Weight(
				entry.name(),
				Double.valueOf(Math.round(DIV * index / maxd) / DIV))
			);
		}
		return result;
	}

}
