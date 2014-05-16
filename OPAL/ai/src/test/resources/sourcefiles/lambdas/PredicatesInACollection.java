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

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.opal.ai.test.invokedynamic.annotations.*;
import static org.opal.ai.test.invokedynamic.annotations.TargetResolution.*;

/**
 * A few cases of lambda-predicates stored in collections.
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
public class PredicatesInACollection {
	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "lambda$localCollection$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 60),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "lambda$localCollection$0", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 61)
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
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 80),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "lambda$static$1", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 81)
	})
	private static void classCollection() {
		System.out.println(predicates.get(0).test(4));
		System.out.println(predicates.get(0).test(Integer.valueOf(4)));
	}

	@InvokedMethods({
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 89),
		@InvokedMethod(resolution = DYNAMIC, receiverType = PredicatesInACollection.class, name = "isNegative", parameterTypes = { Integer.class }, returnType = boolean.class, isStatic = true, lineNumber = 90)
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

