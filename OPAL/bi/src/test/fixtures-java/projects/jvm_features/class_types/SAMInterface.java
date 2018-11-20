/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package class_types;

import java.util.List;

@FunctionalInterface
interface GenericSAMInterface extends MarkerInterface, Cloneable {

    <T> boolean apply(List<T> o);

}

@FunctionalInterface
@SuppressWarnings("all")
interface SignatureCompatibleGenericSAMInterface extends MarkerInterface, Cloneable {

    boolean apply(List o);

}

/**
 * Defines a functional interface/a single abstract method interface as used in the combination
 * with lambda expressions. A functional interface can define arbitrary constants and static
 * methods, but only one abstract instance method.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
@FunctionalInterface
@SuppressWarnings("all")
public interface SAMInterface extends GenericSAMInterface {

    String SIMPLE_STRING_CONSTANT = "CONSTANT";

    String COMPLEX_STRING_CONSTANT = new java.util.Date().toString();

    static boolean testHelper(Object o) {
        return o.getClass() == java.lang.Object.class;
    }

    boolean apply(List o);

    default void printDoIt(){System.out.println("Do it!");}

}

@FunctionalInterface
interface ExtSAMInterface extends SAMInterface {

    default void printExtended(){System.out.println("Extended!");}

}

@FunctionalInterface
interface ExtExtSAMInterface extends ExtSAMInterface, SignatureCompatibleGenericSAMInterface {

    static int max(int a,int b){ return a>b ? a : b; }

    @SuppressWarnings("all")
    boolean apply(List o);

}


// THIS IS NOT A SAMInterface anymore, though it it defines a single abstract method.
// @FunctionalInterface
interface SomeInterface extends SAMInterface {

    boolean call(Object o);

    static void printIt(String s){ System.out.println(s); }
}

@SuppressWarnings("all")
class SAMInterfaceDemo {

    public SAMInterfaceDemo(SAMInterface i) {
        System.out.println(i.apply(null));
    }

    public static SAMInterfaceDemo factory() {
        SAMInterface i =  (List o) -> { return o == null; };
        return new SAMInterfaceDemo(i);
    }

    public static SAMInterfaceDemo factory(Object test) {
        return new SAMInterfaceDemo((List o) -> { return o == test; });
    }

}
