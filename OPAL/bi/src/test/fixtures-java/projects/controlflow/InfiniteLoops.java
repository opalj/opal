/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package controlflow;

/**
 * Methods which contain definitive infinite loops (with complex internal contro-flow).
 * <p>
 * Primarily intended to test the computation of (post-)dominator trees/dominance frontiers and
 * control-dependence graphs.
 *
 * @author Michael Eichberg
 */
public class InfiniteLoops {

    static void justInfiniteLoop(int i) {
        while (true) {
            ;
        }
    }

    static void unboundedLoopWhichMayThrowException(int i) {
        while (true) {
            i = 12122 / i;
        }
    }

    static void infiniteLoopWithMultipleExitPoints(int i) {
        while (true) {
            if (i < 0) {
                i += 1000;
            } else {
                i -= 100;
            }
        }
    }

    static void trivialInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                ;
            }
        }
    }

    static void trivialNestedInfiniteLoops(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                if(i * 1 == i) {
                    while(true) {
                        ;
                    }
                }
            }
        }
    }

    static void regularLoopInInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                for (int j = 0; j < i; j++) {
                    j *= i;
                }
            }
        }
    }

    static void nestedInfiniteLoops(int i) {
        if (i < 0)
            return;
        else {
            while (true) {
                if(i < 10){
                    while(true) {
                        i++;
                    }
                }
            }
        }
    }

    static void complexPathToInfiniteLoop(int i) {
        if (i < 0)
            return;
        else {
            if (i > 0) {
                i = i + 1;
            } else {
                i = -i;
            }
            i = i * i;
            while (true) {
                ;
            }
        }
    }

    static void infiniteLoopWithComplexPath(int i) {
        if (i < 0)
            return;
        else {
            for (; true; ) {
                if (i > 0) {
                    i = i + 1;
                } else {
                    i = -i;
                }
                i = i * i;
            }
        }
    }

    static void complexPathToInfiniteLoopWithComplexPath(int i) {
        if (i < 0)
            return;
        else {
            if (i > 0) {
                i = i + 1;
            } else {
                i = -i;
            }
            i = i * i;
            for (; true; ) {
                int j = i;
                if (j == 0) {
                    j = 1;
                } else {
                    j = -j + 2;
                }
                i = j * i;
            }
        }
    }

    static void multipleInfiniteLoops(int i) {
        if (i == -2332) while (true) { ; }

        if(i < -10000) {
            // The following loop is just included to "test" if the control-dependence graph
            // is non-termination insensitive or sensitive!
            for (int j = -100000; j < i; j++) { ; }
            return;
        }

        if(i > 10000) {
            // The following loop is just included to "test" if the control-dependence graph
            // is non-termination insensitive or sensitive!
            for (int j = 0; j < i; j++) { if(i * j > 1000) return; }
            return;
        }

        if(i >= 0) {
            if(i > 100) {
                while (true) { // basic infinite loop
                    if(i+1 == 0) i--; // this "if" is always false....
                }
            } else if (i < 10 ){
                while (true) { // infinite loop with complex body
                    for (int j = 0 ; j < i; j++) { // regular loop in infinite loop
                        try {
                            while (true) { // only seemingly an infinite loop
                                System.out.println("test");
                            }
                        } catch (Throwable t) {
                            // let's forget about the exception
                        }
                    }
                }
            } else {
                while (true) {
                    i += 1;
                    if(i > 1000) {
                        do { // conditional nested infinite loop
                            i -= 1;
                        } while (true);
                    }
                }
            }
        } else {
            for (int j = -1111 ; j < i ; i++) { // regular loop
                System.out.println(j);
            }
        }
    }
}
