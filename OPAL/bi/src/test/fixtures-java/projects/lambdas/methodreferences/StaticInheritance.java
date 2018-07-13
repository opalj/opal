/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.io.Serializable;
import java.util.Comparator;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains method references to static methods.
 *
 * <!--
 *
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 *
 * -->
 *
 * @author Andreas Muttscheller
 */
public class StaticInheritance {

    public static class A {
        public static String foo() {
            return "bar";
        }
        public static void bar(String s) {
        }
    }

    public static class B extends A {
        /* Empty */
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/StaticInheritance$B", name = "foo", line = 67)
    public static String staticInheritanceTest() {
        Supplier<String> s = B::foo;
        return s.get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/StaticInheritance$B", name = "bar", line = 73)
    public static void staticInheritanceWithParameter() {
        Consumer<String> c = B::bar;
        c.accept("foo");
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/lang/Integer", name = "compareUnsigned", line = 80)
    public Comparator<Integer> makeComparator() {
        // Return a comparator method reference that is also serializable
        return (Comparator<Integer> & Serializable) Integer::compareUnsigned;
    }
}
