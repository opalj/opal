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
package ai.domain;

import java.io.Serializable;

/**
 * A class that does perform a large number of operations related to reference values.
 * 
 * @author Michael Eichberg
 */
public class ReferenceValuesFrenzy {

    static void doIt(Object o) {
        System.out.println(o);
    }

    static void doIt(String id, Object o) {
        System.out.println(id + ":" + o);
    }

    static void printError(Object o) {
        System.err.println(o);
    }

    //
    // Test methods/Test Fixture
    //

    // Returns either "null" or a "new Object"
    static Object maybeNull() {
        if (System.currentTimeMillis() % 100l > 50l)
            return null;
        else
            return new Object();
    }

    @SuppressWarnings("all")
    static Object aliases(Object a, Object b) {
        if (a == null) {
            a = b;
            if (a instanceof java.io.Serializable) {
                return b; // <= this is java.io.Serializable
            }
        } else {
            if (b == null) {
                return a; // non-null
            } else {
                Object d = null;
                if (a.hashCode() < b.hashCode()) {
                    d = a;
                } else {
                    d = b;
                }
                if (d instanceof java.lang.Comparable<?>) {
                    doIt(a); // a non-null Object
                    doIt(b); // a non-null Object
                    return d; // java.lang.Comparable
                }
                return d; // a or b || both not null
            }
        }

        return null;
    }

    static void swap(int index, Object[] values) {
        Object v = values[index];
        values[index] = values[index + 1];
        values[index + 1] = v;
    }

    static Object simpleConditionalAssignment(int i) {
        Object o = null;
        if (i < 0)
            o = new Object();
        else
            o = new Object();

        return o; // o is either one or the other object
    }

    static Object conditionalAssignment1() {
        Object o = null;
        for (int i = 0; i < 2; i++) {
            if (i == 0)
                o = null;
            else
                o = new Object();

            if (o == null && i != 0)
                printError("impossible");
        }

        return o; // o is not null....
    }

    static Object conditionalAssignment2() {
        Object o = null;
        for (int i = 0; i < 2; i++) {
            if (i == 0)
                o = new Object();
            else
                o = null;

            if (o == null && i == 0)
                printError("impossible");
        }

        return o; // o is null....
    }

    static void complexConditionalAssignment1() {
        Object a = null;
        Object b = null;
        Object c = null;
        Object d = null;

        for (int i = 0; i < 3; i++) {
            Object o = maybeNull();
            switch (i) {
            case 0:
                a = o;
                break;
            case 1:
                b = o;
                break;
            case 2:
                c = o;
                break;
            }
        }
        if (a == null) {
            doIt(/* "a: a===null", */a);
            doIt(/* "a: b.isNull.isUnknown", */b);
            doIt(/* "a: c.isNull.isUnknown", */c);
            doIt(/* "a: d===null", */d);
        }
        if (b == null) {
            doIt(/* "b: a.isNull.isUnknown", */a);
            doIt(/* "b: b===null", */b);
            doIt(/* "b: c.isNull.isUnknown", */c);
            doIt(/* "b: d===null", */d);
        }
        if (c == null) {
            doIt(/* "c: a.isNull.isUnknown", */a);
            doIt(/* "c: b.isNull.isUnknown", */b);
            doIt(/* "c: c===null", */c);
            doIt(/* "c: d===null", */d);
        }
    }

    // REQUIRES A CAPABLE IntegerDomain
    static void complexConditionalAssignment2() {
        Object a = null;
        Object b = null;
        Object c = null;
        Object d = null;

        for (int i = 0; i < 2; i++) {
            Object o = maybeNull();
            switch (i) {
            case 0:
                a = o;
                break;
            case 1:
                b = o;
                break;
            case 2: // dead code... 
                c = o;
                break;
            }
        }
        if (a instanceof java.io.Serializable) {
            doIt(a); // java.io.Serializable (or null)
            doIt(b);
            doIt(c);
            doIt(d);
        }
        if (b instanceof java.util.Vector) {
            doIt(a);
            doIt(b);// java.util.Vector (or null)
            doIt(c);
            doIt(d);
        }
        if (c != null) {
            printError("impossible");
        }
    }
}
