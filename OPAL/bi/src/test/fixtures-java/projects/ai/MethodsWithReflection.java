/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

/**
 * Methods that use reflection.
 * 
 * 
 * @author Michael Eichberg
 */
public class MethodsWithReflection {

    public static Class<?> someClass1() throws ClassNotFoundException {
        return Class.forName("ai.MethodsPlain");
    }

    public static Class<?> someClass2() {
        return ai.MethodsPlain.class;
    }
}
