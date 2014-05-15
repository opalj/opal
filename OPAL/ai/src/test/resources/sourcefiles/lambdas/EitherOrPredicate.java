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

import java.util.function.Predicate;

import org.opal.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opal.ai.test.invokedynamic.annotations.InvokedMethods;
import static org.opal.ai.test.invokedynamic.annotations.TargetResolution.*;

/**
 * A simple predicate that matches if and only if exactly one of its parameters matches.
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
public class EitherOrPredicate<T> implements Predicate<T> {
	private Predicate<T> a, b;

	public EitherOrPredicate(Predicate<T> a, Predicate<T> b) {
		this.a = a;
		this.b = b;
	}

	@Override
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = EitherOrPredicate.class, name = "lambda$main$0", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, lineNumber = 67),
		@InvokedMethod(resolution = DYNAMIC, receiverType = String.class, name = "isEmpty", parameterTypes = {}, returnType = boolean.class)
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
