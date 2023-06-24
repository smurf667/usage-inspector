package de.engehausen.inspector;

import java.lang.instrument.Instrumentation;

/**
 * Main agent class. Creates the transformer and adds a shutdown
 * hook for reporting purposes.
 */
public class Agent {

	/**
	 * Entry method for the agent.
	 * @param agentArgs a string with the agent arguments (key value pairs are
	 * expressed as {@code key=value} separated by a colon)
	 * @param instrumentation the instrumentation instance
	 */
	public static void premain(final String agentArgs, final Instrumentation instrumentation) {
		final var transformer = new Transformer(agentArgs);
		instrumentation.addTransformer(transformer);
		Runtime.getRuntime().addShutdownHook(transformer.atShutdown());
	}

}
