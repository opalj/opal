/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * Methods that perform some operation and do some sanitization.
 * 
 * @author Michael Eichberg
 */
@SuppressWarnings("all")
public class Sanitization {

    static void sanitize(String s) { /* do nothing */
    }

    void notSanitized1(String s) {
        return;
    }

    void notSanitized2(String s) {
        if (System.nanoTime() > 0) {
            sanitize(s);
        }
    }

    void sanitized1(String s) {
        sanitize(s);
    }

    void sanitized2(String s) {
        if (System.nanoTime() > 0) {
            sanitize(s);
        } else {
            System.gc();
            sanitize(s);
        }
    }

    void sanitized3(String s) {
        if (s == null) {
            System.gc();
            sanitize(s);
            return;
        }

        if (s != null) {
            sanitize(s);
            return;
        }
    }

    void sanitized4(String s) {
        sanitize(s);
        if (s == null) {
            System.out.println("null");
            return;
        }

        if (s != null) {
            System.out.println(s);
            return;
        }
    }

    void sanitized5(String s) {
        if (s == null) {
            System.gc();
        }
        sanitize(s);
        if (s != null) {
            System.gc();
        }
    }

    void sanitized6(String s) {
        if (s == null) {
            System.gc();
        }
        sanitize(s);
        if (s != null) {
            System.gc();
            return;
        }
    }

    void sanitized7(String s) {
        if (s == null) {
            System.gc();
            sanitize(s);
        }

        if (s != null) {
            sanitize(s);
        }
    }
}
