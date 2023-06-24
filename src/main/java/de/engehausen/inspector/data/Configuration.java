package de.engehausen.inspector.data;

import java.util.List;

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
