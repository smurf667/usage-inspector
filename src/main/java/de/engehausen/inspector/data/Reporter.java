package de.engehausen.inspector.data;

import java.util.Map;

/**
 * Reporter interface to output different types of reports.
 * 
 * @param <T> the report type
 */
public interface Reporter<T> {

	/** Name of the reporter. */
	String name();

	/**
	 * Transforms the given report into a different representation
	 * that will be serialized using Jackson.
	 * @param report the report.
	 * @param meta a map of additional configuration data.
	 * @return the report to serialize.
	 */
	T transform(Report report, Map<String, Object> meta);

}
