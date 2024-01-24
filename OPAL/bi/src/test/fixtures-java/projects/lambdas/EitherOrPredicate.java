/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.function.Predicate;

import annotations.target.InvokedMethod;
import annotations.target.InvokedMethods;
import static annotations.target.TargetResolution.*;

/**
 * A simple predicate that matches if and only if exactly one of its parameters matches.
 *
 * DO NOT RECOMPILE SINCE LAMBDA METHODS ARE COMPILER GENERATED, SO THE GIVEN NAMES MIGHT CHANGE!
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
public class EitherOrPredicate<T> implements Predicate<T> {
	private Predicate<T> a, b;

	public EitherOrPredicate(Predicate<T> a, Predicate<T> b) {
		this.a = a;
		this.b = b;
	}

	@Override
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/EitherOrPredicate", name = "lambda$main$0", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, line = 67),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/String", name = "isEmpty", parameterTypes = {}, returnType = boolean.class)
	})
	public boolean test(T t) {
		boolean A = a.test(t), B = b.test(t);
		return (A && !B) || (!A && B);
	}

	public static void main(String[] args) {
		Predicate<String> tautology = new EitherOrPredicate<>(String::isEmpty, (String s) -> !s.isEmpty());
		for (String arg : args) {
			System.out.printf("Argument %s matches: %b%n", arg, tautology.test(arg));
		}
	}
}
