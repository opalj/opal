/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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

    private static final void check(int i) throws IllegalArgumentException {
        if (i < 0)
            throw new IllegalArgumentException();
    }

    private static final void log(Throwable t) {
        System.err.println(t);
    }

    private static final void doIt() throws InterruptedException {
        Thread.sleep(1000);
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

    static long complexCodeWithNewLocalVarsAfterfinally(int i, int j, long z) throws Exception {
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
}
