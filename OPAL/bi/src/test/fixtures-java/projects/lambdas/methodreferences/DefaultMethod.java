/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package lambdas.methodreferences;

import annotations.target.InvokedMethod;
import static annotations.target.TargetResolution.DYNAMIC;

/**
 * This class contains an example of a method reference dealing with interface default methods.
 *
 * <!--
 * <p>
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * <p>
 * -->
 *
 * @author Andreas Muttscheller
 */
public class DefaultMethod {

    @FunctionalInterface public interface FIBoolean {

        boolean get();
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "java/util/Enumeration", name = "hasMoreElements",  line = 54)
    public static <T> boolean interfaceReference(java.util.Enumeration<T> enum1) {
        FIBoolean bc = enum1::hasMoreElements;

        return bc.get();
    }

    public interface IDefaultMethod {

        default boolean foo() {
            return true;
        }
    }

    @InvokedMethod(resolution = DYNAMIC, receiverType = "lambdas/methodreferences/DefaultMethod$IDefaultMethod", name = "foo",  line = 68)
    public static boolean defaultMethodInterface(IDefaultMethod idm) {
        FIBoolean bc = idm::foo;
        return bc.get();
    }
}
