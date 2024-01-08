/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UninitializedFieldAccessDuringStaticInitialization;

/**
 * This test covers the analysis' ability to track the code-path-specific initialization
 * status of static fields.
 * 
 * @author Daniel Klauer
 */
public class CodePaths {

    public static int condition10 = 0;
    public static int condition11 = 0;
    public static int condition12 = 0;
    public static int condition20 = 0;
    public static int condition21 = 0;
    public static int condition22 = 0;
    public static int condition30 = 0;
    public static int condition31 = 0;
    public static int condition40 = 0;

    static void test11() {
        if (condition11 != 0) {
        } else {
            System.out.println(CodePathsSubclass.i11); // uninitialized
        }
    }

    static void test40() {
        if (condition40 != 0) {
            System.out.println(CodePathsSubclass.i40); // uninitialized
            return;
        }

        System.out.println(CodePathsSubclass.i40); // uninitialized
    }

    static {
        // Uninitialized access on only 1 of 2 code paths
        if (condition10 != 0) {
            System.out.println(CodePathsSubclass.i10); // uninitialized
        } else {
        }

        // same but in a subroutine
        test11();

        // Uninitialized accesses on both code paths
        if (condition12 != 0) {
            System.out.println(CodePathsSubclass.i12); // uninitialized
        } else {
            System.out.println(CodePathsSubclass.i12); // uninitialized
        }

        // Field initialized manually, but only on 1 of 2 code paths,
        // potentially causing an uninitialized access later
        if (condition20 != 0) {
            CodePathsSubclass.i20 = 1;
        }
        System.out.println(CodePathsSubclass.i20); // maybe uninitialized

        if (condition21 != 0) {
            CodePathsSubclass.i21 = 1;
        } else {
            CodePathsSubclass.i21 = 2;
        }
        System.out.println(CodePathsSubclass.i21); // ok

        if (condition22 != 0) {
            CodePathsSubclass.i22 = 1;
            System.out.println(CodePathsSubclass.i22); // ok
        }

        // Field initialized manually on one code path,
        // and accessed uninitialized on the other.
        if (condition30 != 0) {
            CodePathsSubclass.i30 = 1;
        } else {
            System.out.println(CodePathsSubclass.i30); // uninitialized
            // (if the analysis didn't track code paths, but just looked through
            // instructions one by one, this would probably not be detected as an
            // uninitialized access.
        }

        if (condition31 != 0) {
            System.out.println(CodePathsSubclass.i31); // uninitialized
        } else {
            CodePathsSubclass.i31 = 1;
        }

        test40();
    }
}

class CodePathsSubclass extends CodePaths {

    static int i10 = 42;
    static int i11 = 42;
    static int i12 = 42;
    static int i20 = 42;
    static int i21 = 42;
    static int i22 = 42;
    static int i30 = 42;
    static int i31 = 42;
    static int i40 = 42;
}
