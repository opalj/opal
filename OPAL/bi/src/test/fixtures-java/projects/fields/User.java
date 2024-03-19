/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package fields;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class User {

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        // basically characterizes the interesting use cases
        System.out.println(Super.x);
        System.out.println(SubI.THE_I);

        Sub sub = new Sub();
        System.out.println(sub.x);
        System.out.println(sub.THE_I);
        System.out.println(sub.THE_SUB_I);

        SubSub subSub = new SubSub();
        System.out.println(subSub.x);

    }

}
