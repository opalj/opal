package dataflowTest;

public final class InterproceduralFlowTest {

	@Sink
	public static void sink(Object target) {
		// All the evil you can think of
	}

	/***
	 * Simple identity
	 * 
	 * @param target
	 * @return
	 */
	private static Object identity(Object target) {
		return target;
	}

	/***
	 * Simple null constant on any parameter
	 * 
	 * @param target
	 * @return
	 */
	private static Object makeNull(Object target) {
		return null;
	}

	/***
	 * Source taking a step through an identity function before leaking
	 * 
	 * @param target
	 */
	@Source
	public static void identityStep(Object target) {
		sink(identity(target));
	}

	@Source
	public static void noFlow(Object target) {
		sink(makeNull(target));
	}

	@Source
	public static void simpleRecursion(Object target, int n) {
		if (n > 0)
			simpleRecursion(target, n - 1);
		else
			sink(target);
	}

	@Source
	public static void crossRecursion(Object target, int n) {
		crossRecursionInt1(target, n);
	}

	private static void crossRecursionInt1(Object target, int n) {
		if (n > 0)
			crossRecursionInt2(target, n - 1);
		else
			sink(target);
	}

	private static void crossRecursionInt2(Object target, int n) {
		if (n > 0)
			crossRecursionInt1(target, n - 1);
		else
			sink(target);
	}

	
	@Source
	public static void theOnlyfooCaller(Object target) {
		foo(null,target);
	}
	
	private static void foo(Object irr, Object target) {
		if (irr != null)
			sink(target);
	}
}
