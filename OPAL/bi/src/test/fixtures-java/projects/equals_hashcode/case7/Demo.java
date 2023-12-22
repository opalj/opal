/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package equals_hashcode.case7;

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
        NoHashCodeOverrideNoSuperCheckSuper sub1 = new NoHashCodeOverrideNoSuperCheckSub(
                1);
        NoHashCodeOverrideNoSuperCheckSuper sub2 = new NoHashCodeOverrideNoSuperCheckSub(
                2);

        System.out.println("sub1.equals(sub2): " + sub1.equals(sub2)); // true
        System.out.println("sub1.hashCode(): " + sub1.hashCode()); // 32
        System.out.println("sub2.hashCode(): " + sub2.hashCode()); // 33
    }

}
