package de.engehausen.inspector.data;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Class usage information. Contains the total number of method
 * calls and optionally a mapping with counts per method.
 * @param totalCalls the number of total calls for the class
 * @param methodCalls a map with counts for individual methods (optional)
 */
@JsonInclude(JsonInclude.Include.NON_NULL) 
public record ClassInfo(int totalCalls, Map<String, AtomicInteger> methodCalls) {}
