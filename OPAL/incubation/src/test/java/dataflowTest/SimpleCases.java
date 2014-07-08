package dataflowTest;

public final class SimpleCases {
	@Sink
	public static void sink(Object target) {
		// Here the evil things may happen. Mark this as a sink.
	}

	/**
	 * Source with constant target passed to sink.
	 */
	@Source
	public static void constantSource() {
		Object target = new Object();

		sink(target);
	}

	/***
	 * Source with parameter plainly passed to sink
	 * 
	 * @param target
	 */
	@Source
	public static void passingSource(Object target) {
		sink(target);
	}

	/***
	 * Source passing a parameter only on a non-trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void conditionalSource(Object target) {
		if (System.currentTimeMillis() % 1000 == 0)
			sink(target);
	}

	/***
	 * Source passing a parameter in a loop
	 * 
	 * @param target
	 */
	@Source
	public static void loopingSource(Object target) {
		for (int i = 0; i < 10; i++)
			sink(target);
	}

	/***
	 * Source passing a parameter in a loop only on a non-trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void conditionalLoopingSource(Object target) {
		if (System.currentTimeMillis() % 1000 == 0)
			for (int i = 0; i < 10; i++)
				sink(target);
	}

	/***
	 * Source passing a parameter, but preceded by a trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void trivialConditionalSource(Object target) {
		if (true)
			sink(target);
	}
	
	/***
	 * Source not passing a parameter, but preceded by a trivial condition
	 * 
	 * @param target
	 */
	@Source
	public static void trivialConditionalCounterexampleSource(Object target) {
		if (false)
			sink(target);
	}

	/***
	 * Source passing a parameter on all cases of a branch
	 * 
	 * @param target
	 */
	@Source
	public static void allConditionsLeak(Object target) {
		if (System.currentTimeMillis() % 1000 == 0) {
			sink(target);
		} else if (System.currentTimeMillis() % 365 == 0) {
			sink(target);
		} else {
			sink(target);
		}
	}

	/***
	 * Source passing a parameter on some cases of a branch
	 * 
	 * @param target
	 */
	@Source
	public static void someConditionsLeak(Object target) {
		if (System.currentTimeMillis() % 1000 == 0) {
			sink(target);
		} else if (System.currentTimeMillis() % 365 == 0) {
			// do nothing
		} else {
			sink(target);
		}
	}
	
	@Source
	public static void leakOnException(Object target) {
		try {
			int i;
			if (target == null) 
				i = 12 / 0;
		} catch (Exception e) {
			sink(target);
		}
		
	}

}
