package de.engehausen.inspector.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Quantized extends AbstractWeightMapper {

	public static final String NAME = "quantized";
	/** {@code steps} - the number of steps (>= 2), mapped to 0..1, defaults to 4 */
	public static final String KEY_STEPS = "steps";

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public List<Weight> report(final List<Weight> weights, final Map<String, Object> meta) {
		final double steps = Double.parseDouble(meta.getOrDefault(KEY_STEPS, "4").toString());
		final List<Weight> result = new ArrayList<>(weights.size());
		final int max = weights.size();
		final double maxD = max;
		for (int index = 0; index < max; index++) {
			final Weight weight = weights.get(index);
			result.add(new Weight(weight.name(), Math.round(steps * (1 + index) / maxD) / steps));
		}
		return result;
	}

}
