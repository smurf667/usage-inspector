package de.engehausen.inspector.data;

import java.util.List;
import java.util.Map;

/**
 * Configuration for the agent.
 * @param excludes a list of regular expressions of class names to exclude
 * @param includes a list of regular expressions of class names to include
 * @param details flag to include individual method call counts
 * @param out file name of the report JSON file to write
 * @param reportIssues flag to output instrumentation problems at the end of the VM (output to {@code System.err})
 * @param reporter the name of the reporter to use; if not specified a {@link Report} will be output
 * @param meta meta configuration that may be passed to the reporter
 */
public record Configuration(
	List<String> excludes,
	List<String> includes,
	boolean details,
	String out,
	String reportIssues,
	String reporter,
	Map<String, Object> meta) {

	/** file name of JSON formatted configuration */
	public static String ARG_CONFIG = "config";

	/** comma-separated list of regular expressions for classes to exclude, defaults to none */
	public static String ARG_EXCLUDES = "excludes";
	/** comma-separated list of regular expressions for classes to include, defaults to all */
	public static String ARG_INCLUDES = "includes";
	/** report details (defaults to {@code true}) */
	public static String ARG_DETAILS = "details";
	/** output file name */
	public static String ARG_OUT = "out";
	/** flag to report issues seen during agent operation at the end to stderr */
	public static String ARG_REPORT_ISSUES = "reportIssues";
	/** name of the reporter to use */
	public static String ARG_REPORTER = "reporter";
	/** filename of meta configuration in JSON format */
	public static String ARG_META = "meta";

}
