package de.engehausen.inspector.data;

import java.util.List;

/**
 * Configuration for the agent.
 * @param excludes a list of regular expressions of class names to exclude
 * @param includes a list of regular expressions of class names to include
 * @param details flag to include individual method call counts
 * @param out file name of the report JSON file to write
 * @param reportIssues flag to output instrumentation problems at the end of the VM (output to {@code System.err})
 */
public record Configuration(List<String> excludes, List<String> includes, boolean details, String out, String reportIssues) {

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

}
