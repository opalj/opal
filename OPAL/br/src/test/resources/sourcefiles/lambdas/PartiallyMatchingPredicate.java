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

import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;
import static org.opalj.ai.test.invokedynamic.annotations.TargetResolution.*;

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
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * 
 * 
 * -->
 *
 * @author Arne Lottmann
 */
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
		@InvokedMethod(resolution = DYNAMIC, receiverType = String.class, name = "isEmpty", parameterTypes = {}, returnType = boolean.class),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PartiallyMatchingPredicate.class, name = "lambda$main$0", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, lineNumber = 78),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PartiallyMatchingPredicate.class, name = "lambda$main$1", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, lineNumber = 78),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PartiallyMatchingPredicate.class, name = "lambda$main$2", parameterTypes = {String.class}, returnType = boolean.class, isStatic = true, lineNumber = 78),
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
