/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package cornercases;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public interface MethodSignatures {

    void main(String[] args);

    void main(Object o, boolean b, double d);

    void main(double d, Object o);

    void main(Object o, boolean b, double d, Object o2);

    void main(boolean b, double d);

    void main(double d, float f, String[] args, long l);

    void main(boolean b, double d, float f, String[] args, long l, int i);

}

class MethodSignaturesCaller {

    MethodSignatures ms = null;

    void doIt1() {
        ms.main(new String[0]);
    }

    void doIt2() {
        ms.main(new Object(), true, 1.0d);
    }

    void doIt3() {
        ms.main(-1.0d, new Object());
    }

    void doIt4() {
        ms.main("Object o", false, 2.333d, "Object o2");
    }

    void doIt5() {
        ms.main(true, 33.33333d);
    }

    void doIt6() {
        ms.main(212.0d, 1.0f, new String[] { "a", "b" }, 12121l);
    }

}
