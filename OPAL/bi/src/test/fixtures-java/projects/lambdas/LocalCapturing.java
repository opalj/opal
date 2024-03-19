/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.*;

/**
 * A few lambdas to demonstrate capturing of local variables.
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
public class LocalCapturing {
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/LocalCapturing", name = "lambda$capturePrimitive$0", isStatic = true, line = 55)
	public void capturePrimitive() {
		int x = 1;
		Runnable r = () -> System.out.println(x);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/LocalCapturing", name = "lambda$captureObject$1", isStatic = true, line = 62)
	public void captureObject() {
		String s = "string";
		Runnable r = () -> System.out.println(s);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/LocalCapturing", name = "lambda$captureArray$2", isStatic = true, line = 69)
	public void captureArray() {
		int[] a = new int[] { 1 };
		Runnable r = () -> { for (int i : a) System.out.println(i); };
		r.run();
	}
}
