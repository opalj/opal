/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.*;

/**
 * A few lambdas to demonstrate capturing of instance variables.
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
public class InstanceCapturing {
	private int x = 1;
	
	private String s = "string";
	
	private int[] a = new int[] { 1 };
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/InstanceCapturing", name = "lambda$capturePrimitive$0", line = 60)
	public void capturePrimitive() {
		Runnable r = () -> System.out.println(x);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/InstanceCapturing", name = "lambda$captureObject$1", line = 66)
	public void captureObject() {
		Runnable r = () -> System.out.println(s);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/InstanceCapturing", name = "lambda$captureArray$2", line = 72)
	public void captureArray() {
		Runnable r = () -> { for (int i : a) System.out.println(i); };
		r.run();
	}
}
