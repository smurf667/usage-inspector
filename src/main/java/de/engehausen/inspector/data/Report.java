package de.engehausen.inspector.data;

import java.util.Map;

public record Report(Map<String, ClassInfo> classes) {}
