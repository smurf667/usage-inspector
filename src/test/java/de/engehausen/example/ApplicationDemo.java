package de.engehausen.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ApplicationDemo {

	private static int COUNTER = 0;
	
	private static boolean once() {
		return true;
	}

	private static int twice() {
		return ++COUNTER;
	}

	private int factorial(final int v) {
		return v < 2 ? v : v * factorial(v - 1);
	}

	@Test
	void performCalls() {
		Assertions.assertTrue(once());
		Assertions.assertEquals(1, twice());
		Assertions.assertEquals(2, twice());
	}

	@Test
	void performRecursion() {
		Assertions.assertEquals(120, factorial(5));
	}
}
