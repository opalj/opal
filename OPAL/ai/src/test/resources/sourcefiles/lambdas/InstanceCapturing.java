/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package lambdas;

import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import static org.opalj.ai.test.invokedynamic.annotations.TargetResolution.*;

/**
 * A few lambdas to demonstrate capturing of instance variables.
 *
 * DO NOT RECOMPILE SINCE LAMBDA METHODS ARE COMPILER GENERATED, SO THE GIVEN NAMES MIGHT CHANGE!
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
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
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = InstanceCapturing.class, name = "lambda$capturePrimitive$0", lineNumber = 60)
	public void capturePrimitive() {
		Runnable r = () -> System.out.println(x);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = InstanceCapturing.class, name = "lambda$captureObject$1", lineNumber = 66)
	public void captureObject() {
		Runnable r = () -> System.out.println(s);
		r.run();
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = InstanceCapturing.class, name = "lambda$captureArray$2", lineNumber = 72)
	public void captureArray() {
		Runnable r = () -> { for (int i : a) System.out.println(i); };
		r.run();
	}
}
