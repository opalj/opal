/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai;

import java.io.PrintStream;

/**
 * Just a very large number of methods that contain loops.
 *
 *
 * @author Michael Eichberg
 */
public class MethodsWithLoops {

    public static void singleStepLoop() {
        for (int i = 0; i < 1; i++) {
            System.out.println(i);
        }
    }

    public static void twoStepsLoop() {
        for (int i = 0; i < 2; i++) {
            System.out.println(i);
        }
    }

    public static void countTo10() {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }
    }

    public static void countToM10() {
        for (int i = 0; i > -10; i--) {
            System.out.println(i);
        }
    }

    public static void endlessDueToBug() {
        int i = 1;
        while (i < 2) {
            System.out.println(System.nanoTime());
            i -= 1;
        }
    }

    public static Object iterateList(java.util.List<?> list) {
        java.util.List<?> l = list;
        while (l != null) {
            l = (java.util.List<?>) l.get(0);
        }
        return list.toString();
    }

    public static void endless() {
        while (true) {
            System.out.println(System.nanoTime());
        }
    }

    @SuppressWarnings("all")
    public static void endless(boolean toErr) {
        PrintStream out = toErr ? System.err : System.out;
        while (true) {
            out.println(System.nanoTime());
        }
    }

    public static void eventuallyEndless() {
        singleStepLoop();
        for (int i = 0; i > -10; i--) {
            System.out.println(i);
        }
        // the loop...
        while (true) {
            long t = System.nanoTime();
            if (t % 333 == 0)
                System.out.println(System.nanoTime());
        }
    }

    public static void conditionallyEndless() {
        // the method may return normally or run forever ...
        if (System.nanoTime() > 10000333222l)
            while (true) {
                long t = System.nanoTime();
                if (t % 333 == 0)
                    System.out.println(System.nanoTime());
            }
    }
}
