package de.engehausen.inspector.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps the classes of the input report to a list of weights
 * according to the quantized percentile of each class (0..1)
 * in steps defined be {@link #KEY_STEPS} (default: 4).
 */
public class Quantized extends AbstractWeightMapper {

	/** {@code quantized} */
	public static final String NAME = "quantized";
	/** {@code steps} - the number of steps (>= 2), mapped to 0..1, defaults to 4 */
	public static final String KEY_STEPS = "steps";

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
		final double steps = Double.parseDouble(meta.getOrDefault(KEY_STEPS, "4").toString());
		final List<Weight> result = new ArrayList<>(weights.size());
		final int max = weights.size();
		final double maxD = max;
		for (int index = 0; index < max; index++) {
			final Weight weight = weights.get(index);
			result.add(new Weight(weight.path(), Math.round(steps * (1 + index) / maxD) / steps));
		}
		return result;
	}

}
