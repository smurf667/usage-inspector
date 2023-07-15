package de.engehausen.inspector.reporters;

import java.util.Map;

import de.engehausen.inspector.data.Report;
import de.engehausen.inspector.data.Reporter;

public class Identity implements Reporter<Report> {

	public static final String NAME = "identity";

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
	public Report transform(final Report report, final Map<String, Object> meta) {
		return report;
	}

}
