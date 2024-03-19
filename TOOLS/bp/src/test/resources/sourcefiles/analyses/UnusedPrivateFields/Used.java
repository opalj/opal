/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package UnusedPrivateFields;

/**
 * A class containing various used private fields, testing that the analysis detects field
 * accesses to normal instructions and handles the many special cases for final constant
 * fields.
 * 
 * @author Daniel Klauer
 */
public class Used {

    private int normali = 0;

    private final int im1 = -1;
    private final int i0 = 0;
    private final int i1 = 1;
    private final int i2 = 2;
    private final int i3 = 3;
    private final int i4 = 4;
    private final int i5 = 5;
    private final int i6 = 6;
    private final int i123 = 123;

    private final long lm1 = -1;
    private final long l0 = 0;
    private final long l1 = 1;
    private final long l2 = 2;
    private final long l3 = 3;
    private final long l4 = 4;
    private final long l5 = 5;
    private final long l6 = 6;
    private final long l123 = 123;

    private final float fm1 = -1;
    private final float f0 = 0;
    private final float f1 = 1;
    private final float f2 = 2;
    private final float f3 = 3;
    private final float f4 = 4;
    private final float f5 = 5;
    private final float f6 = 6;
    private final float f123 = 123;

    private final double dm1 = -1;
    private final double d0 = 0;
    private final double d1 = 1;
    private final double d2 = 2;
    private final double d3 = 3;
    private final double d4 = 4;
    private final double d5 = 5;
    private final double d6 = 6;
    private final double d123 = 123;

    private final String s = "abc";

    void test() {
        System.out.println(normali);

        System.out.println(im1);
        System.out.println(i0);
        System.out.println(i1);
        System.out.println(i2);
        System.out.println(i3);
        System.out.println(i4);
        System.out.println(i5);
        System.out.println(i6);
        System.out.println(i123);

        System.out.println(lm1);
        System.out.println(l0);
        System.out.println(l1);
        System.out.println(l2);
        System.out.println(l3);
        System.out.println(l4);
        System.out.println(l5);
        System.out.println(l6);
        System.out.println(l123);

        System.out.println(fm1);
        System.out.println(f0);
        System.out.println(f1);
        System.out.println(f2);
        System.out.println(f3);
        System.out.println(f4);
        System.out.println(f5);
        System.out.println(f6);
        System.out.println(f123);

        System.out.println(dm1);
        System.out.println(d0);
        System.out.println(d1);
        System.out.println(d2);
        System.out.println(d3);
        System.out.println(d4);
        System.out.println(d5);
        System.out.println(d6);
        System.out.println(d123);

        System.out.println(s);
    }
}
