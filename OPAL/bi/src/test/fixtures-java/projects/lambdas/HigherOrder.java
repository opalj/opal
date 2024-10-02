/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import java.util.function.Predicate;

import annotations.target.InvokedMethod;
import annotations.target.InvokedMethods;
import static annotations.target.TargetResolution.*;

/**
 * A few test cases exploring "higher order" and nested lambdas.
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
public class HigherOrder {
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/HigherOrder", name = "lambda$higherOrderPredicate$0", parameterTypes = {Predicate.class}, returnType = boolean.class, isStatic = true, line = 60),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/HigherOrder", name = "lambda$higherOrderPredicate$1", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, line = 59),
	})
	public void higherOrderPredicate() {
		Predicate<Predicate<String>> acceptsEmptyString = (Predicate<String> p) -> p.test("");
		acceptsEmptyString.test((String s) -> s.isEmpty());
	}
	
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/HigherOrder", name = "lambda$nestedPredicate$3", parameterTypes = { String.class }, returnType = boolean.class, isStatic = true, line = 75),
		@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/HigherOrder", name = "lambda$null$2", parameterTypes = { Character.class }, returnType = boolean.class, isStatic = true, line = 71)
	})
	public void nestedPredicate() {
		Predicate<String> outer = (String s) -> {
			Predicate<Character> inner = (Character c) -> c > 31;
			for (char c : s.toCharArray()) {
				if (!inner.test(c)) return false;
			}
			return true;
		};
		outer.test("foobar");
	}
}
