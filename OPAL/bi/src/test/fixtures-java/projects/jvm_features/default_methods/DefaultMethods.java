/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package default_methods;

/**
 * The following method defines certain default methods and alse demonstrates
 * resolution of "maximally-specific" methods.
 */
interface BaseInterface  {

    default void printDoIt(){System.out.println("BaseInterface");}

}

interface SubBaseInterfaceA extends BaseInterface {

    default void printDoIt(){System.out.println("SubBaseInterfaceA");}

}

interface SubSubBaseInterfaceA extends SubBaseInterfaceA {

    // does not refine the default method printDoIt

}

interface SubBaseInterfaceB extends BaseInterface, SubBaseInterfaceA {

    // here, SubBasedInterfaceA.printDoIt is the maximally specific method

}

class BaseInterfaceDemo {

    public static void main(String[] args) {
        new SubBaseInterfaceB(){}.printDoIt();
    }

}
