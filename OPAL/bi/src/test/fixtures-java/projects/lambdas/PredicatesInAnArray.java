/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.function.Predicate;

import annotations.target.InvokedMethod;
import annotations.target.InvokedMethods;
import static annotations.target.TargetResolution.*;

/**
 * A few cases of lambda-predicates stored in arrays.
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * @author Arne Lottmann
 */
@SuppressWarnings("unchecked")
public class PredicatesInAnArray {
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "lambda$localArray$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 60),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "lambda$localArray$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 61)
	})
	private static void localArray() {
		Predicate<Integer> p = (Integer i) -> i < 5;
		Predicate<Integer>[] predicates = (Predicate<Integer>[]) new Predicate[] {
			p
		};
		System.out.println(predicates[0].test(4));
		System.out.println(predicates[0].test(Integer.valueOf(4)));
	}

	private static boolean isNegative(Integer i) {
		return i.compareTo(0) < 0;
	}

	private static Predicate<Integer>[] predicates;
	static {
		Predicate<Integer> p = (Integer i) -> i > 3;
		Predicate<Integer> p2 = PredicatesInAnArray::isNegative;
		predicates = new Predicate[] { p, p2 };
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 80),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 81)
	})
	private static void classArray() {
		System.out.println(predicates[0].test(4));
		System.out.println(predicates[0].test(Integer.valueOf(4)));
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 89),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInAnArray", name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 90)
	})
	private static void paramArray(Predicate<Integer>[] predicates) {
		System.out.println(predicates[1].test(2));
		System.out.println(predicates[1].test(Integer.valueOf(2)));
	}

	public static void main (String[] args) {
		localArray();
		classArray();
		paramArray(predicates);
	}
}
