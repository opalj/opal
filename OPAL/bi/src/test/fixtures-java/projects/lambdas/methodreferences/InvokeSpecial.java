/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import java.util.function.Supplier;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains examples for method references which result in INVOKESPECIAL calls.
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
 * @author Andreas Muttscheller
 */
public class InvokeSpecial {

    public static class Superclass {
        private String interestingMethod() {
            return "Superclass";
        }

        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/InvokeSpecial$Superclass", name = "$forward$interestingMethod", line = 58)
        public void exampleMethodTest() {
            Supplier<String> s = this::interestingMethod; // reference of a private method
            s.get();
        }

        protected String someMethod() {
            return "someMethod";
        }
    }

    public static class Subclass extends Superclass {

        String interestingMethod() {
            return "Subclass";
        }

        // name = "access$0", because of the inheritance of superclass. someMethod is accessed
        // via this access$0 method.
        @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/InvokeSpecial$Subclass", name = "access$0", line = 77)
        public String callSomeMethod() {
            Supplier<String> s = super::someMethod; // reference of a super method
            return s.get();
        }
    }

    public static void staticInheritanceWithParameter() {
        Subclass sc = new Subclass();
        sc.exampleMethodTest();
        sc.callSomeMethod();
    }
}
