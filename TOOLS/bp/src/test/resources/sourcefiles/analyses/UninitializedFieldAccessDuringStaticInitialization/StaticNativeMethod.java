/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package UninitializedFieldAccessDuringStaticInitialization;

/**
 * A class that calls a `static native` method during `<clinit>`. This is special because
 * references to `native` methods can be resolved, but they can still not be analyzed
 * because they don't have known bodies.
 * 
 * This test class must have a subclass with a static field, or else the analysis would
 * simply skip it immediately.
 * 
 * @author Daniel Klauer
 */
public class StaticNativeMethod {

    public static native int f();

    static {
        System.out.println(f());
        System.out.println(StaticNativeMethodSubclass.i);
    }
}

class StaticNativeMethodSubclass extends StaticNativeMethod {

    static int i = 123;
}
