/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case2;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class Demo {

    public static void main(String[] args) {
        IgnoreFieldInEquals obj1 = new IgnoreFieldInEquals(1);
        IgnoreFieldInEquals obj2 = new IgnoreFieldInEquals(2);

        System.out.println("obj1.equals(obj2): " + obj1.equals(obj2)); // true
        System.out.println("obj1.hashCode(): " + obj1.hashCode()); // 32
        System.out.println("obj2.hashCode(): " + obj2.hashCode()); // 33
    }

}
