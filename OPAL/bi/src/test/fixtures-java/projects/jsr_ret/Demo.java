/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package jsr_ret;

/**
 * This class was used to create a class file which has subroutines/has JSR/RET statements.
 * The produced code contains nested subroutines (up to three levels deep) as well as
 * sequences of immediately following JSR instructions (`m5`)
 * <p>
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
final public class Demo extends Object {

    private static Object[] data = new Object[]{new Object()};

    private static final Object get() throws IllegalArgumentException {
        return new Object();
    }

    private static final void check(int i) throws IllegalArgumentException {
        if (i < 0)
            throw new IllegalArgumentException();
    }

    private static final void check(Object i) throws IllegalArgumentException {
        if (i == null) {
            throw new IllegalArgumentException();
        }
    }

    private static final void log(Throwable t) {
        System.err.println(t);
    }

    private static final void doIt() throws InterruptedException {
        Thread.sleep(1000);
    }

    public static final int m0(int j) throws Exception {
        int r = 0;
        try {
            try {
                check(j);
            } finally {
                try {
                    check( j * 100);
                } finally {
                    // terminates inner subroutine
                    if(j*100 == System.currentTimeMillis())
                        throw new Throwable();
                    r = 100;
                }
                return 100 / j;
            }
        } catch (Throwable t) {
            check(t);
        }
        check(r);
        return r;
    }

    public static final int m1(int j) throws Exception {
        try {
            check(j);
        } finally {
            try {
                j = j * j;
                check(j);
            } finally {
                System.out.println(j);
            }
            return j;
        }
    }

    public static final int m2(int j) throws Exception {
        try {
            check(j);
            doIt();
        } finally {
            System.out.println("the final countdown");
            try {
                j = j * j;
                check(j);
            } finally {
                System.out.println(j);
            }
        }
        doIt();
        return 1;
    }

    public static final int m3(int j) throws Exception {
        try {
            check(j);
            doIt();
        } finally {
            System.out.println("the final countdown");
            try {
                j = j * j;
                check(j);
            } finally {
                System.out.println(j);
                if (j % 2 == 0)
                    return 0;
            }
        }
        doIt();
        return 1;
    }

    public static final int m4(int j) throws Exception {
        int y = -j + 100;
        try {
            check(j + 1);
            doIt();
            int z = 0;
            try {
                j = j * j;
                check(j);
            } finally {
                System.out.println(j);
                if (j % 4 == 0)
                    return 0;
                else
                    z = 4;
            }
            z = j + 3;
        } catch (IllegalArgumentException iae) {
            log(iae);
            y *= 3;
            throw iae;
        } catch (InterruptedException ie) {
            log(ie);
            y += 4;
            throw ie;
        } finally {
            System.out.println("the final countdown");
            try {
                j = j * j;
                check(j);
            } finally {
                System.out.println(j);
                if (j % 2 == 0)
                    return y % j;
            }
        }
        doIt();
        return y;
    }

    public static final int m5(int j) throws Exception {
        int y = -j + 100;
        int z = 0;

        try {
            doIt();
            if (y > 10) {
                try {
                    j = j * j;
                    check(j);
                    return j % 1000;
                } finally {
                    if (j % 4 == 0)
                        return 0;
                    else
                        z = 4;
                }
            }
            z = j + 3;
        } catch (IllegalArgumentException iae) {
            log(iae);
            y *= 3;
            throw iae;
        } catch (InterruptedException ie) {
            log(ie);
            y += 4;
            throw ie;
        } finally {
            try {
                j = j * j;
                check(j);
            } finally {
                if (j % 2 == 0)
                    return y % j;
            }
        }
        doIt();
        return y;
    }

    public static final int m6(int j) throws Exception {
        int y = -j;
        int z = y + j;

        try {
            if (y > 10) {
                try {
                    check(j);
                    return j;
                } finally {
                    if (j == 4)
                        return 0;
                }
            }
        } finally {
            try {
                check(j);
            } finally {
                if (y == 93)
                    return z;
            }
        }
        return y;
    }

    static long complexCodeWithNewLocalVarsAfterFinally(int i, int j, long z) throws Exception {
        if (i < j + z)
            return -1;
        try {
            if (j == i) {
                try {
                    // we do a lot of things that potentially throw exceptions..
                    int k = i / j;
                    String s = "" + k + z;
                    System.out.println(s.replace('0', '1'));
                    if (k + z == 0) {
                        throw new IllegalArgumentException();
                    }
                } finally {
                    doIt();
                    System.out.println("we reached this point! Yeah!");
                    doIt();
                }
            } else if (j != i) {
                try {
                    // we do a lot of things that potentially throw exceptions..
                    int u = i % j;
                    String t = "" + u;
                    System.out.println(t.replace('-', '+'));
                } finally {
                    doIt();
                }
            }
            String theS = " i < j ";
            System.out.println(theS);
        } finally {
            z = j * j * z / i;
        }

        int k1 = m1(j);
        int k2 = k1 + m2(k1);
        long l1 = k1 + m2(k1) + z;
        long l3 = m4((int) l1);
        long l4 = l1 * l3;
        return l1 * l3 * l4;
    }

    public int finallyGame(Object g) throws Throwable {
        Object o;
        Object z;
        try {
            Object io = null;
            try {
                this.equals(g.toString());
            } finally {
                try {
                    System.out.println("Did it - 1!");
                } finally {
                    o = new Object();
                    System.err.println("Everything is falling apart!");
                }
                io = new Object();
            }
            System.out.println(io.toString());
        } finally {
            z = "z";
            System.out.println("Did it -2!");
        }
        return z.hashCode() + o.hashCode();
    }

    public static int finallyAndCatchGame(Object g) throws Throwable {
        Object o;
        Object z;
        try {
            System.out.println("before o assinemnt...");
            o = new Object();
        } catch (java.lang.RuntimeException re){
            o = "re";
        } catch (java.lang.Exception e){
            o = "e";
        } finally {
            o = "finally";
            z = "z";
            System.out.println("Did it -2!");
        }
        return z.hashCode() + o.hashCode();
    }

    public static int tryCatchFinallyInInfiniteLoop(Object g) throws Throwable {
        int i = 0;
        while(true) {
            try {
                System.out.println(i);
            } catch (Throwable t) {
                i -= 1;
            } finally {
                i += 1;
            }
        }
    }

    public static int[] implicitAndExplicitTryCatchFinally(int i) throws Exception {
        synchronized(Demo.get()) {
            String s = "";
            if(i == -1) throw new RuntimeException();
            Object[] data = Demo.data;

            try {
                int[] counts = null;
                if(i == 0) {
                    counts = new int[]{i,i+1};
                    RuntimeException e = null;
                    Object[] otherData = null;
                    Object t = null;
                    try {
                        for (int c = 0; c < counts.length ; c++) {
                            int count = counts[c];
                            try {
                                if(count == 0) {
                                    check(count);
                                }
                                try {
                                    counts[count] = m1(count);
                                } finally {
                                    otherData = Demo.data;
                                }
                                if(m1(1000*i) == 19999){
                                    java.io.InputStream in = null;
                                    try {
                                        in = (java.io.InputStream) get();
                                    } finally {
                                        if (in != null) {
                                            in.close();
                                        }
                                    }
                                }
                            } catch (RuntimeException re) {
                                if(m1(66+i)== 766)
                                    e = re;
                                else
                                    throw new IllegalArgumentException();
                            }
                        }
                    }finally {
                        m1(666);
                    }
                    if(e != null) {
                        throw new RuntimeException(counts.toString());
                    }
                }

                return counts;
            } finally {
                Demo.data = data;
            }
        }
    }
}
