package de.engehausen.inspector.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps the classes of the input report to a list of weights
 * according to either 0 or 1 depending on
 * the threshold defined via {@link #KEY_LIMIT} (default: 0.5)
 * of the percentiles for each class.<br>
 * This means: "Below threshold == unimportant, weight 0" and
 * "above == important, weight 1".
 */
public class Threshold extends AbstractWeightMapper {

	/** {@code quantized} */
	public static final String NAME = "threshold";
	/** {@code limit} - the threshold limit (0..1), defaults to 0.5 */
	public static final String KEY_LIMIT = "limit";

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
		final int cutOff = (int) (((double) weights.size()) * Double.parseDouble(meta.getOrDefault(KEY_LIMIT, "0.5").toString()));
		final List<Weight> result = new ArrayList<>(weights.size());
		for (int index = weights.size(); --index >= 0; ) {
			final Weight weight = weights.get(index);
			result.add(new Weight(weight.name(), Integer.valueOf(index >= cutOff ? Integer.valueOf(1) : Integer.valueOf(0))));
		}
		return result;
	}

}
