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

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static annotations.target.TargetResolution.*;


/**
 * This class contains a few simple examples for method references introduced in Java 8.
 *
 * <!--
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
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
	}

	public int inferredTypeArgs() {
    	@SuppressWarnings("rawtypes")
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

	public String superToString() {
    	java.util.function.Supplier<String> s = new Child().getSuperToString();
    	return s.get();
	}

	public String overloadResolution() {
    	java.util.function.Function<Double, String> f = String::valueOf;
    	return f.apply(3.14);
	}

	public int[] typeArgsFromContext() {
		java.util.function.Consumer<int[]> c = Arrays::sort;

		int[] someInts = {3, 2, 9, 14, 7};

		c.accept(someInts);
		return someInts;
	}

	public int[] typeArgsExplicit() {
		java.util.function.Consumer<int[]> c = Arrays::<int[]>asList;

		int[] someInts = {3, 2, 9, 14, 7};

		c.accept(someInts);
		return someInts;
	}

	public java.util.ArrayList<String> parameterizedConstructor() {
    	java.util.function.Supplier<java.util.ArrayList<String>> s =
				java.util.ArrayList<String>::new;
    	return s.get();
	}

    @SuppressWarnings("rawtypes")
	public java.util.ArrayList inferredConstructor() {
		java.util.function.Supplier<java.util.ArrayList> s = java.util.ArrayList::new;
		return s.get();
	}

	public GenericConstructor genericConstructor() {
    	java.util.function.Function<String, GenericConstructor> f = GenericConstructor::<String>new;
    	return f.apply("42");
	}

	public GenericClass<String> genericClass() {
		java.util.function.Function<String, GenericClass<String>> f =
				GenericClass<String>::<String>new;
		return f.apply("42");
	}

	public Outer.Inner nestedClass() {
    	java.util.function.Supplier<Outer.Inner> s = Outer.Inner::new;
    	return s.get();
	}

	public int[] arrayNew() {
    	java.util.function.Function<Integer, int[]> f = int[]::new;
    	return f.apply(42);
	}

	public Double sumDoubleDouble() {
    	java.util.function.BiFunction<Double, Double, Double> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
    	return bf.apply(2d, 2d);
	}

	public Double sumDoubleFloat() {
		java.util.function.BiFunction<Double, Float, Double> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
		return bf.apply(2d, 3.14f);
	}

	public Double sumFloatDouble() {
		java.util.function.BiFunction<Float, Double, Double> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
		return bf.apply(4.2f, 2d);
	}

	public Float sumFloatFloat() {
		java.util.function.BiFunction<Float, Float, Float> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
		return bf.apply(2.7f, 2.5f);
	}

	public Double sumFloatDoubleFloat() {
		TriFunction<Float, Double, Float, Double> tf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
		return tf.apply(3.14f, 42d, 2.5f);
	}

	public Double sumDoubleInt() {
		java.util.function.BiFunction<Integer, Double, Double> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
		return bf.apply(42, 42d);
	}

	public Double sumLongDouble() {
        java.util.function.BiFunction<Long, Double, Double> bf =
                lambdas.MethodReferences.MixedDoubleParamters::sum;
        return bf.apply(42l, 42.3d);
    }

    public Long sumLongLong() {
        java.util.function.BiFunction<Long, Long, Long> bf =
                lambdas.MethodReferences.MixedLongParamters::sum;
        return bf.apply(42l, 42l);
    }

    public Long sumLongInt() {
        java.util.function.BiFunction<Long, Integer, Long> bf =
                lambdas.MethodReferences.MixedLongParamters::sum;
        return bf.apply(42l, 24);
    }

    public Long sumIntInt() {
        java.util.function.BiFunction<Integer, Integer, Long> bf =
                lambdas.MethodReferences.MixedLongParamters::sum;
        return bf.apply(42, 314);
    }

    public Double sumDoubleLong() {
        java.util.function.BiFunction<Double, Long, Double> bf =
                lambdas.MethodReferences.MixedLongParamters::sum;
        return bf.apply(5.5d, 42l);
    }

    public Long sumIntLongLong() {
        TriFunction<Integer, Long, Long, Long> tf =
                lambdas.MethodReferences.MixedLongParamters::sum;
        return tf.apply(3, 4l, 5l);
    }

    public static <T, R> R someBiConsumerParameter(
    		java.util.function.Supplier<R> s,
    		java.util.function.BiConsumer<R, T> bc,
			java.util.function.BiConsumer<R, R> r,
			T t) {
		R state = s.get();
		bc.accept(state, t);
		r.accept(state, state);

		return state;
	}

	public static <T> LinkedHashSet<T> callBiConsumer(T t) {
		LinkedHashSet<T> lhm = MethodReferences.<T, LinkedHashSet<T>>someBiConsumerParameter(
				LinkedHashSet::new,
				LinkedHashSet::add,
				LinkedHashSet::addAll,
				t
		);

		return lhm;
	}

	public static class MixedDoubleParamters {
    	public static double sum(double a, double b) {
    		return a+b;
		}

		public static double sum(double a, float b) {
    		return a+b;
		}

		public static double sum(float a, double b) {
			return a+b;
		}

		public static float sum(float a, float b) {
    		return a+b;
		}

		public static double sum(float a, double b, float c) {
    		return a+b+c;
		}

		public static double sum(int a, double b) {
    		return a+b;
		}

		public static double sum(long a, double b) {
    	    return a+b;
        }
	}

    public static class MixedLongParamters {
        public static long sum(long a, long b) {
            return a+b;
        }

        public static long sum(long a, int b) {
            return a+b;
        }

        public static long sum(int a, int b) {
            return a+b;
        }

        public static double sum(double a, long b) {
            return a+b;
        }

        public static long sum(int a, long b, long c) {
            return a+b+c;
        }
    }

	public static class Outer {
    	public static class Inner {

		}
	}

	public static class GenericConstructor {
    	Object p;
    	<T> GenericConstructor(T param) {
    		p = param;
		}
	}

	public static class GenericClass<T> {
    	Object p;
    	T q;
    	<U> GenericClass(U param) {
    		p = param;
		}
	}

	public static class Parent {
    	@Override
    	public String toString() {
    		return "Parent";
		}
	}

	public static class Child extends Parent {
    	public java.util.function.Supplier<String> getSuperToString() {
    		return super::toString;
		}

		@Override
		public String toString() {
    		return "Child";
		}
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

	@FunctionalInterface
	interface TriFunction<A,B,C,R> {
		R apply(A a, B b, C c);
	}
}
