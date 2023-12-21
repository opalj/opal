/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.function.Predicate;

import annotations.target.InvokedMethod;
import annotations.target.InvokedMethods;
import static annotations.target.TargetResolution.*;


/**
 * Simple predicate implementation to test resolution of method references / lambdas that have
 * been stores in an array.
 *
 * DO NOT RECOMPILE SINCE LAMBDA METHODS ARE COMPILER GENERATED, SO THE GIVEN NAMES MIGHT CHANGE!
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * -->
 * @author Arne Lottmann
 */
@SuppressWarnings("unchecked")
public class PartiallyMatchingPredicate<T> implements Predicate<T> {
	private Predicate<T>[] predicates;
	private double threshold;

	public PartiallyMatchingPredicate(double threshold, Predicate<T>... predicates) {
		this.predicates = predicates;
		this.threshold = threshold;
	}

	@Override
	public boolean test(T t) {
		double score = getScore(t);
		return score >= threshold;
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "/java/lang/String", name = "isEmpty", parameterTypes = {}, returnType = boolean.class),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PartiallyMatchingPredicate", name = "lambda$main$0", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, line = 78),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PartiallyMatchingPredicate", name = "lambda$main$1", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, line = 78),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/PartiallyMatchingPredicate", name = "lambda$main$2", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, line = 78),
	})
	public double getScore(T t) {
		int successful = 0;
		for (Predicate<T> p : predicates) {
			if (p.test(t)) {
				successful++;
			}
		}
		double score = ((double) successful) / predicates.length;
		return score;
	}
	
	public static void main(String[] args) {
		PartiallyMatchingPredicate<String> p = new PartiallyMatchingPredicate<>(0.7,
				String::isEmpty,
				((Predicate<String>) String::isEmpty).negate(),
				(String s) -> s.length() > 2,
				(String s) -> s.toUpperCase().equals("FOO"));
		for (String arg : args) {
			System.out.printf("%s matches: %b; score: %.2f%n", arg, p.test(arg), p.getScore(arg));
		}
	}
}
