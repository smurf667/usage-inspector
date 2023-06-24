package de.engehausen.inspector;

import java.lang.instrument.Instrumentation;

public class Agent {

	public static void premain(final String agentArgs, final Instrumentation instrumentation) {
		final var transformer = new Transformer(agentArgs);
		instrumentation.addTransformer(transformer);
		Runtime.getRuntime().addShutdownHook(transformer.atShutdown());
	}

}
