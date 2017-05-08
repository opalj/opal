/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

import annotations.target.InvokedMethod;

import java.util.ArrayList;
import java.util.Iterator;

import static annotations.target.TargetResolution.*;


/**
 * This class contains a few simple examples for method references introduced in Java 8.
 *
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * 
 *
 *
 *
 * -->
 *
 * @author Arne Lottmann
 */
public class MethodReferences {
    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/MethodReferences$Value", name = "isEmpty", line = 52)
	public void filterOutEmptyValues() {
		java.util.List<Value> values = java.util.Arrays.asList(new Value("foo"), new Value(""));
		values.stream().filter(Value::isEmpty);
	}

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/MethodReferences$Value", name = "compare", line = 58, isStatic = true)
	public void compareValues() {
		java.util.Comparator<Value> comparator = Value::compare;
		System.out.println(comparator.compare(new Value("a"), new Value("b")));
	}
	
	public interface ValueCreator {
		Value newValue(String value);
	}
	
	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/MethodReferences$Value", name = "<init>", line = 68)
	public Value newValue(String value) {
		ValueCreator v = Value::new;
		return v.newValue(value);
	}

	// @InvokedMethod(...)
	public int instanceMethod() {
		java.util.function.Function<String, Integer> i = String::length;
    	return i.apply("instanceMethod");

	}

	// @InvokedMethod(...)
	public long staticMethod() {
		java.util.function.LongSupplier t = System::currentTimeMillis;
		return t.getAsLong();
	}

	public int explicitTypeArgs() {
		java.util.function.Function<java.util.List<String>, Integer> t =
				java.util.List<String>::size;
		java.util.ArrayList<String> stringArray = new java.util.ArrayList<>();
		stringArray.add("1");
		stringArray.add("2");
		return t.apply(stringArray);
	}

	public void partialBound(java.util.List<Object> someList) {
    	// add(int index, E element)
		java.util.function.BiConsumer<Integer, Object> s = someList::add;
		s.accept(0, new Object());

		java.lang.Runnable r = someList::add(0, new Object());

		// TODO: Method reference currying, one parameter bound ?
		// TODO: Functions with 2-3 parameters including double and long and mixed
	}

	public int inferredTypeArgs() {
    	//TODO: @SuppressWarnings()
		java.util.function.Function<java.util.List, Integer> t = java.util.List::size;
		java.util.ArrayList<String> stringArray = new java.util.ArrayList<>();
		stringArray.add("1");
		stringArray.add("2");
		return t.apply(stringArray);
	}

	public int[] intArrayClone() {
    	int[] intArray = {0, 1, 2, 42};
    	java.util.function.Function<int[], int[]> t = int[]::clone;

    	return t.apply(intArray);
	}

	public int[][] intArrayArrayClone() {
		int[][] intArray = {{0, 1, 2, 42}};
		java.util.function.Function<int[][], int[][]> t = int[][]::clone;

		return t.apply(intArray);
	}

	public boolean objectMethod() {
    	Value v = new Value("foo");
    	java.util.function.BooleanSupplier t = v::isEmpty;
    	return t.getAsBoolean();
	}

	public void referencePrintln() {
    	java.util.function.Consumer<String> c = System.out::println;
    	c.accept("Hello World!");
	}

	public int referenceLength() {
    	java.util.function.Supplier<Integer> s = "foo"::length;
    	return s.get();
	}

	public int arrayMethod() {
		String[] stringArray = {"0", "1", "2", "42"};

		java.util.function.IntSupplier s = stringArray[0]::length;

		return s.getAsInt();
	}

	public Iterator<String> ternaryIterator(boolean t) {
		java.util.ArrayList<String> stringArray1 = new java.util.ArrayList<>();
		stringArray1.add("1");
		stringArray1.add("2");
		java.util.ArrayList<String> stringArray2 = new java.util.ArrayList<>();
		stringArray2.add("foo");
		stringArray2.add("bar");

		java.util.function.Supplier<Iterator<String>> f =
				(t ? stringArray1 : stringArray2) :: iterator;

		return f.get();
	}

	public static class Value {
		private String value;

		public Value(String value) {
			this.value = value;
		}

		public boolean isEmpty() {
			return value.isEmpty();
		}

		public static int compare(Value a, Value b) {
			return a.value.compareTo(b.value);
		}
	}
}	
