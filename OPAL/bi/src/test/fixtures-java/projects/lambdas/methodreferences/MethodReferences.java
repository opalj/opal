/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import java.util.*;
import java.util.function.*;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.*;

/**
 * This class contains a few simple examples for method references introduced in Java 8.
 *
 * <!--
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 * -->
 * @author Arne Lottmann
 */
public class MethodReferences {
    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Value", name = "isEmpty", line = 52)
	public void filterOutEmptyValues() {
		List<Value> values = Arrays.asList(new Value("foo"), new Value(""));
		values.stream().filter(Value::isEmpty);
	}

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Value", name = "compare", line = 58, isStatic = true)
	public void compareValues() {
		Comparator<Value> comparator = Value::compare;
		System.out.println(comparator.compare(new Value("a"), new Value("b")));
	}

	public interface ValueCreator {
		Value newValue(String value);
	}

	@InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Value", name = "<init>", line = 68)
	public Value newValue(String value) {
		ValueCreator v = Value::new;
		return v.newValue(value);
	}

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/String", name = "length", line = 73)
    public int instanceMethod() {
        Function<String, Integer> i = String::length;
        return i.apply("instanceMethod");

    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/System", name = "currentTimeMillis", line = 80)
    public long staticMethod() {
        LongSupplier t = System::currentTimeMillis;
        return t.getAsLong();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/List", name = "size", line = 86)
    public int explicitTypeArgs() {
        Function<List<String>, Integer> t = List<String>::size;
        ArrayList<String> stringArray = new ArrayList<>();
        stringArray.add("1");
        stringArray.add("2");
        return t.apply(stringArray);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/List", name = "add", parameterTypes = { int.class, Object.class }, line = 96)
    public void partialBound(List<Object> someList) {
        // add(int index, E element)
        BiConsumer<Integer, Object> s = someList::add;
        s.accept(0, new Object());
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/List", name = "size", line = 102)
    public int inferredTypeArgs() {
        @SuppressWarnings("rawtypes") Function<List, Integer> t = List::size;
        ArrayList<String> stringArray = new ArrayList<>();
        stringArray.add("1");
        stringArray.add("2");
        return t.apply(stringArray);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences", name = "lambda$8", line = 112)
    public int[] intArrayClone() {
        int[] intArray = { 0, 1, 2, 42 };
        Function<int[], int[]> t = int[]::clone;

        return t.apply(intArray);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences", name = "lambda$9", line = 120)
    public int[][] intArrayArrayClone() {
        int[][] intArray = { { 0, 1, 2, 42 } };
        Function<int[][], int[][]> t = int[][]::clone;

        return t.apply(intArray);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Value", name = "isEmpty", line = 120)
    public boolean objectMethod() {
        Value v = new Value("foo");
        BooleanSupplier t = v::isEmpty;
        return t.getAsBoolean();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/io/PrintStream", name = "println", line = 134)
    public void referencePrintln() {
        Consumer<String> c = System.out::println;
        c.accept("Hello World!");
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/String", name = "length", line = 140)
    public int referenceLength() {
        Supplier<Integer> s = "foo"::length;
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/String", name = "length", line = 148)
    public int arrayMethod() {
        String[] stringArray = { "0", "1", "2", "42" };

        IntSupplier s = stringArray[0]::length;

        return s.getAsInt();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/ArrayList", name = "iterator", line = 164)
    public Iterator<String> ternaryIterator(boolean t) {
        ArrayList<String> stringArray1 = new ArrayList<>();
        stringArray1.add("1");
        stringArray1.add("2");
        ArrayList<String> stringArray2 = new ArrayList<>();
        stringArray2.add("foo");
        stringArray2.add("bar");

        Supplier<Iterator<String>> f = (t ?
                stringArray1 :
                stringArray2)::iterator;

        return f.get();
    }

    public String superToString() {
        Supplier<String> s = new Child().getSuperToString();
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/String", name = "valueOf", parameterTypes = { Object.class }, line = 176)
    public String overloadResolution() {
        Function<Double, String> f = String::valueOf;
        return f.apply(3.14);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/Arrays", name = "sort", parameterTypes = { int[].class }, line = 182)
    public int[] typeArgsFromContext() {
        Consumer<int[]> c = Arrays::sort;

        int[] someInts = { 3, 2, 9, 14, 7 };

        c.accept(someInts);
        return someInts;
    }


    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences", name = "lambda$25", line = 192)
    public int[] typeArgsExplicit() {
        // THE FOLLOWING CALL IS NOT COMPILED CORRECTLY; THE GENERATED BYTECODE IS INVALID!
        // Consumer<int[]> c = Arrays::<int[]>asList;
        Function<int[],java.util.List<int[]>> c = Arrays::<int[]>asList;

        int[] someInts = { 3, 2, 9, 14, 7 };

        c.apply(someInts);
        return someInts;
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/ArrayList", name = "<init>", parameterTypes = {  }, line = 202)
    public ArrayList<String> parameterizedConstructor() {
        Supplier<ArrayList<String>> s = ArrayList<String>::new;
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/ArrayList", name = "<init>", parameterTypes = {  }, line = 208)
    @SuppressWarnings("rawtypes") public ArrayList inferredConstructor() {
        Supplier<ArrayList> s = ArrayList::new;
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$GenericConstructor", name = "<init>", line = 214)
    public GenericConstructor genericConstructor() {
        Function<String, GenericConstructor> f = GenericConstructor::<String>new;
        return f.apply("42");
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$GenericClass", name = "<init>", line = 220)
    public GenericClass<String> genericClass() {
        Function<String, GenericClass<String>> f = GenericClass<String>::<String>new;
        return f.apply("42");
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Outer$Inner", name = "<init>", line = 226)
    public Outer.Inner nestedClass() {
        Supplier<Outer.Inner> s = Outer.Inner::new;
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences", name = "lambda$23", line = 232)
    public int[] arrayNew() {
        Function<Integer, int[]> f = int[]::new;
        return f.apply(42);
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/Object", name = "toString", parameterTypes = {  }, line = 239)
    public String arrayInstanceMethod() {
        Object[] someObjects = new Object[10];
        Supplier<String> s = someObjects::toString;
        return s.get();
    }

    public interface SomeInterface {
        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$SomeInterface", name = "$forward$lambda$0", parameterTypes = {  }, line = 246)
        default Runnable foo() {
            return () -> System.out.println("Hello world! " + getSomeInt());
        }

        default int getSomeInt() {
            return 2;
        }

        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$SomeInterface", name = "lambda$1", parameterTypes = {  }, line = 255)
        static Runnable StaticFoo() {
            return () -> System.out.println("Hello world! " + SomeInterface.StaticGetSomeInt());
        }

        static int StaticGetSomeInt() {
            return 2;
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

        @Override public String toString() {
            return "Parent";
        }
    }

    public static class Child extends Parent {
		public static int compare(Value a, Value b) {
			return a.value.compareTo(b.value);
		}

        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/MethodReferences$Child", name = "access$0", line = 275)
        public Supplier<String> getSuperToString() {
            return super::toString;
        }

        @Override public String toString() {
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
}
