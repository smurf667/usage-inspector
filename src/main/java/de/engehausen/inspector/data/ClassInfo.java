package de.engehausen.inspector.data;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) 
public record ClassInfo(int totalCalls, Map<String, AtomicInteger> methodCalls) {}
