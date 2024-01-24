/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import annotations.target.InvokedMethod;
import annotations.target.InvokedMethods;
import static annotations.target.TargetResolution.*;


/**
 * A few cases of lambda-predicates stored in collections.
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 *
 * @author Arne Lottmann
 */
public class PredicatesInACollection {
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "lambda$localCollection$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 60),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "lambda$localCollection$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 61)
	})
	private static void localCollection() {
		Predicate<Integer> p = (Integer i) -> i < 5;
		List<Predicate<Integer>> predicates = Arrays.asList(p);
		System.out.println(predicates.get(0).test(4));
		System.out.println(predicates.get(0).test(Integer.valueOf(4)));
	}

	private static boolean isNegative(Integer i) {
		return i.compareTo(0) < 0;
	}

	private static List<Predicate<Integer>> predicates;
	static {
		Predicate<Integer> p = (Integer i) -> i > 3;
		Predicate<Integer> p2 = PredicatesInACollection::isNegative;
		predicates = Arrays.asList(p, p2);
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 80),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 81)
	})
	private static void classCollection() {
		System.out.println(predicates.get(0).test(4));
		System.out.println(predicates.get(0).test(Integer.valueOf(4)));
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 89),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PredicatesInACollection", name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, line = 90)
	})
	private static void paramCollection(List<Predicate<Integer>> predicates) {
		System.out.println(predicates.get(1).test(2));
		System.out.println(predicates.get(1).test(Integer.valueOf(2)));
	}

	public static void main (String[] args) {
		localCollection();
		classCollection();
		paramCollection(predicates);
	}
}

