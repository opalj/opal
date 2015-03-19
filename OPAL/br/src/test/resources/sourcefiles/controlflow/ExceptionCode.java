/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package controlflow;

import java.io.IOException;

public class ExceptionCode {

    int simpleException(int index) {
        int[] array = { 1, 2, 3, 4 };
        int res = 0;

        try {
            res = array[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        }

        return res;
    }

    @SuppressWarnings("finally")
    int multipleCatchAndFinally(int index) {
        int[] array = { 1, 2, 3, 4 };
        int res = 0;

        try {
            res = 5 / array[index];
        } catch (ArrayIndexOutOfBoundsException e) {
            res = -1;
        } catch (ArithmeticException e) {
            res = -1;
        } finally {
            return res;
        }
    }

    int nestedExceptions(int index) {
        int[] array = { 0, 1, 2, 3, 4, 5 };
        int res = 0;

        try {
            int divisor = array[index];
            try {
                res = 6 / divisor;
            } catch (ArithmeticException e) {
                return -1;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            try {
                int newDivisor = array[index + 1];
                res = 6 / newDivisor;
            } catch (ArithmeticException e2) {
                return -1;
            }
        }

        return res;
    }

    @SuppressWarnings("finally")
    int loopExceptionWithFinallyReturn(int[] array, int a) {

        int res = 0;

        try {
            res = array[a];
        } catch (ArrayIndexOutOfBoundsException e) {
            for (int i = 0; i < array.length; i++)
                res += array[i];
        } finally {
            return res;
        }
    }

    int loopExceptionWithCatchReturn(int[] array, int a) {
        int res = 0;

        try {
            res = array[a];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        } finally {
            for (int i = 0; i < array.length; i++)
                res += array[i];
        }

        return res;
    }

    @SuppressWarnings("all")
    int highlyNestedFinally(int[] array, int a) {
        int res = 0;

        try {
            res += array[a];
            try {
                res += array[a + 1];
                try {
                    res += array[a + 2];
                    try {
                        res += array[a + 3];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        res--;
                    } finally {
                        return res;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    res--;
                } finally {
                    return res;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                res--;
            } finally {
                return res;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            res--;
        } finally {
            return res;
        }
    }

    @SuppressWarnings("unused")
    void tryFinally(int input) {
        int x = 100;

        try {
            x = x / input;
        } finally {
            int a = 1;
            int b = 2;

            if (x * (a + b) > 50) {
                int d = 3;
                int e = 4;
            } else {
                int f = 5;
                int g = 6;
            }
        }

        return;
    }

    static void nestedCatch(java.io.InputStream in) {
        try {
            in.read();
            try {
                in.available();
            } catch (IOException ioe1) {
                try {
                    in.close();
                } catch (IOException ioe2) {
                    System.out.println("there is nothing we can do...");
                    throw ioe2;
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
