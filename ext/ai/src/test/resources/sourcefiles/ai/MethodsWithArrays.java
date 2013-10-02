/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package ai;

/**
 * Methods that create, initialize and update arrays.
 * 
 * <h2>NOTE</h2> This class is not meant to be (automatically) recompiled; it just serves
 * documentation purposes. The compiled class that is used by the tests is found in the
 * test-classfiles directory.
 * 
 * @author Michael Eichberg
 */
public class MethodsWithArrays {

    public static byte byteArrays(byte[] values) {
        int length = values.length;
        values[length / 2] = 10;
        return values[length - 1];
    }

    public static boolean booleanArrays(boolean[] values) {
        int length = values.length;
        values[length / 2] = false;
        return values[length - 1];
    }

    //
    // COMPARISON

    public static Object[] wrap(java.io.Serializable o) {
        if (o == null)
            return new Object[0];
        else
            return new java.io.Serializable[] { o };
    }

    public static boolean instanceofAndArrays(java.io.Serializable o) {
        Object result = wrap(o);
        if (result instanceof java.io.Serializable[]) {
            return true;
        }
        if (result instanceof Object[]) {
            return false;
        } else {
            throw new RuntimeException();
        }
    }

    public static int[] doIt() {
        int[] a = new int[10];
        int i = 0;
        while (i < 10) {
            a[i] = i;
            i = i + 1;
        }
        return a;
    }

    public static int[] doItAbs(int initial) {
        int i = initial;
        int[] a = new int[10];
        while (i < 10) {
            a[i] = i;
            i = i + 1;
        }
        return a;
    }

    public static void main(String[] args) {
        final java.io.PrintStream out = System.out;

        Object o = new java.util.ArrayList[0];
        Object s = new java.io.Serializable[10][5];

        out.println(o instanceof Object[]); // true
        out.println(o instanceof java.io.Serializable[]); // true
        out.println(o instanceof java.util.List[]); // true

        out.println(s instanceof Object[]);
        out.println(s instanceof java.io.Serializable[]);
        out.println(s instanceof java.lang.Cloneable[]);

        out.println(o instanceof java.util.Set[]); // false
        out.println(o instanceof int[]);// false
    }
}
