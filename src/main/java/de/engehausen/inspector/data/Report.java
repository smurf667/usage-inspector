package de.engehausen.inspector.data;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Usage report.
 * @param classes a mapping of class name to usage information
 * @param meta unstructured meta data (may be {@code null})
 */
public record Report(
	Map<String, ClassInfo> classes,
	@JsonInclude(Include.NON_NULL) Map<String, Object> meta) {}
