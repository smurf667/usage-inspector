package de.engehausen.inspector.data;

import java.util.Map;

/**
 * Usage report.
 * @param classes a mapping of class name to usage information
 */
public record Report(Map<String, ClassInfo> classes) {}
