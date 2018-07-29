/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

import ai.domain.IntegerValuesFrenzy;

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

    static Object paramAsVar(Object o) {
        do {
            o = new Object();
        } while (o.hashCode() % 3 == 0);
        return o;
    }

    static Object handlingExceptions(Object o) {
        try {
            o = new Object();
        } catch (Exception e) {
            return o/*this is the parameter...*/;
        }
        return o;
    }

    static boolean complexRefinement() {
        // get the lastChild of result.
        Object lastChild = maybeNull();

        Object prev = lastChild;
        while (lastChild != null) {
            prev = lastChild;
            lastChild = maybeNull();
        }
        // ... here, lastChild is always null, but prev can be both
        lastChild = prev; // pc:19 load und pc:20 store

        if (lastChild != null) {
            return false; // pc:26
        }
        return true; // pc:28
    }

    static void multipleReferenceValues(String s) {
        int i = IntegerValuesFrenzy.anInt();
        String v = "v";
        String u = v;
        if (i == 0) {
            u = s;
        } else if (i == 1) {
            u = null;
        }
        if (u != null) {
            doIt(s); // we don't know to which value u refers to; hence s maybe
                     // null!
            doIt(u); // u is not null and hence is guaranteed to not refer to
                     // the "null" value
        } else
            return;

        // u is not null...
        if (s == null)
            return;

        doIt(u);
        // if u refers to s; s is now non-null
    }

    static void refiningNullnessOfMultipleReferenceValues() {
        int i = IntegerValuesFrenzy.anInt();
        Object n = maybeNull();
        Object o = maybeNull();
        Object m = i == 0 ? n : o;
        if (m == null) {
            doIt(n); // pc:27
            doIt(o); // pc:31

            // here (in this branch) m, may still refer to "n" and "o"
            // (m - as a whole - has to be null, but we
            // don't know to which value m is referring to)
            doIt(m); // pc:35

            if (o != null) {
                doIt(n); // pc:43
                doIt(o); // pc:47

                // here(in this branch) m only refers to "n" which is `null`
                doIt(m); // pc:51
            }
        }
    }

    static void refiningTypeBoundOfMultipleReferenceValues() {
        // in the following we are using excepion types, because this
        // information is always readily available (even if the JDK
        // is not loaded.)
        int i = IntegerValuesFrenzy.anInt();
        Object n = new LinkageError("...");
        Object o = maybeNull();
        Object m = i == 0 ? n : o;
        if (m instanceof Throwable) {
            Object s = (Throwable) m;
            doIt(n); // pc: 40
            doIt(o); // pc: 44

            // here (in this branch) m(s), may still refer to "n" and "o"
            doIt(m); // pc: 48

            if (s instanceof Exception) {
                Exception ms = (Exception) s;
                doIt(n); // pc: 67
                doIt(o); // pc: 71

                // here(in this branch) ms only refers to "o"
                doIt(ms); // pc: 76
            }
        }
    }

    static void relatedMultipleReferenceValues(String s) {
        int i = IntegerValuesFrenzy.anInt();
        Object o = maybeNull(); // pc: 4
        Object p = maybeNull(); // pc: 8
        Object q = maybeNull(); // pc: 12

        Object a = null; // pc: 17
        Object b = null; // pc: 20

        switch (i) {
        case 1:
            a = o;
            b = p;
            break;
        case 2:
            a = p;
            b = o;
            break;
        default:
            a = q;
        }
        // Let's assume that o, p and q are different objects.
        //
        // Now, both: a and b may refer to o and p; however, they will never
        // refer to the same object; hence, any constraints will not affect
        // both values!

        if (a == null) { // pc[load:a]:70
            doIt(b); // b is either the original value, o or p
        }
        if (b == null) {
            doIt(a); // pc: 87 // a is either o, p or q
        } else {
            doIt(a); // pc: 95 // a is either o or p (and both may be null)
            // But, if we are not able track the fact that a can
            // only be non null if i was 1 or 2, then a may also
            // refer to q
        }

        if (o != null) {
            doIt(a); // pc:104:: o should be non-null
            doIt(b); // pc:109:: o should be non-null
        }
    }

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
                return b; // Serializable
            }
        } else {
            if (b == null) {
                return a; // non-null
            } else {
                // a and b are non-null
                Object d = null;
                if (a.hashCode() < b.hashCode()) {
                    d = a;
                } else {
                    d = b;
                }
                if (d instanceof java.lang.Comparable<?>) {
                    doIt(a); // a non-null Object
                    doIt(b); // a non-null Object
                    return d; // d is of type java.lang.Comparable
                }
                return d; // a or b || both not null
            }
        }

        return null;
    }

    static Object complexAliasing(Object a) {
        Object o = a;
        do {
            if (o != null)
                break;
            else
                o = maybeNull();
        } while (IntegerValuesFrenzy.anInt() % 2 == 1);
        doIt(a); // a.isNull === Unknown
        return o; // o.isNull === Unknown; but if o is a then o is "non-null"
    }

    static Object iterativelyUpdated(Object a) {
        do {
            if (a != null) {
                doIt(a); // pc:5 "a" may refer to: the parameter (which is then not null)
                         // the return value of maybeNull
            } else
                a = maybeNull();
        } while (IntegerValuesFrenzy.anInt() % 2 == 1);
        return a; // pc 25 ... if a is (still) the parameter, then a must be non-null
    }

    static void cfDependentValues(int i) {
        Object b = null;
        Object c = null;
        int j = i; // <--- j is just an alias for i
        while (j < 2) {
            Object a = maybeNull();
            if (i == 1)
                b = a; // <--- b is just an alias for a
            else
                c = a; // <--- c is just an alias for a
            i++;
            j = i;
        }
        // b and c are (potentially) not referring to the same object
        if (c == null) { // this just constraints "c" (not "b")
            doIt(b); // we know nothing about b here
            doIt(c); // c is "null"
        } else if (b != null) {
            doIt(b); // b is not null
            doIt(c); // c is not null
        }
    }

    static void swap(int index, Object[] values) {
        Object v = values[index];
        values[index] = values[index + 1];
        values[index + 1] = v;
    }

    static Object simpleConditionalAssignment(int i) {
        Object o = null;
        if (i < 0)
            o = new Object(); // pc[new]: 6
        else
            o = new Object(); // pc[new]: 17

        return o; // pc:26 o is either one or the other object
    }

    static Object conditionalAssignment1() {
        Object o = null;
        for (int i = 0; i < 2; i++) {
            if (i == 0)
                o = null; // pc:11
            else
                o = new Object(); // pc[new]: 16

            if (o == null && i != 0)
                printError("impossible"); // pc[call of printError]:34
        }
        // To get precise results we need to do loop unrolling...
        return o; // pc:46
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
        // To get precise results we need to do loop unrolling...
        return o; // o is null....
    }

    static void complexConditionalAssignment1() {
        Object a = null;
        Object b = null;
        Object c = null;
        Object d = null;
        // To get precise results we need to do loop unrolling...
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
