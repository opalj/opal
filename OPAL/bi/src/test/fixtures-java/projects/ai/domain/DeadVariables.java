/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package ai.domain;

/**
 * This class contains various methods where some reference to values are dead at some
 * point in time.
 *
 * @author Michael Eichberg
 */
public class DeadVariables {

    static class ControlFlowException extends RuntimeException {

        static final long serialVersionUID = 0xcafebabe;
    };

    public void processIt(Object o) {
        /* EMPTY */
    }

    public Object transformIt(Object o) {
        return o;
    }

    Object someFieldA = null;
    Object someFieldB = null;

    private int someInt = (int) (Math.random() * 100.0d);

    // The following example requires s live variable analysis to determine that the
    // (swallowed) exception is dead.
    public Object java_lang_System_newPrintStream_inspired(Object enc) {
        /*
        1142 private static PrintStream More ...newPrintStream(FileOutputStream fos, String enc) {
        1143    if (enc != null) {
        1144         try {
        1145             return new PrintStream(new BufferedOutputStream(fos, 128), true, enc);
        1146         } catch (UnsupportedEncodingException uee) {}
        1147     }
        1148     return new PrintStream(new BufferedOutputStream(fos, 128), true);
        1149 }
        */
       if (enc != null) {
            try {
                return transformIt(transformIt(enc));
            } catch (RuntimeException re) {}
        }
        return "";
    }

    public Object initialValueIsAlwaysDead(int i) {
        if (i < 1 || i > 10) return null;
        // ... i is now positive
        Object o = null;
        do {
            if(i % 2 == 0)
                o = "a";
            else
                o = "b";
            i += 1;
        } while(i < 100 );

        return o;
    }

    public Object simplyDead(Object o) {
        Object v = o;
        if(System.nanoTime() == 2424124234l) {
            v = "also updated";
        } else {
            if (v.hashCode() == 101212) {
                System.out.println(v);
            } else {
                v = "updated";
                System.out.println(v);
            }
        }
        return null;
    }

    public StringBuilder lastStringBuilder(Object o) {
        StringBuilder s = null;
        for(int i = 1; i < 2 ; i++){
            s = new StringBuilder();
            s.append(i);
            System.out.println(s.toString());
        }
        return s; // here... the last created string builder in the loop will be returned...
    }


    public int lastPrimitiveValue(Object o) {
        int s = o.hashCode();
        for(int i = 1; i < 2 ; i++){
            s = 100;
        }
        return s;
    }


    public Object notDeadRestored(Object o) {
        int s = o.hashCode();
        int t = s;
        for(int i = 1; i < 3 ; i++){
            // this loop will be evaluated at least once AND
            // will always kill s BUT
            // at the end will also restore s HENCE
            // when the loop ends, the original value
            // is stored in S
            s = 100;
            s *= i;
            this.someInt = s;
            s = t;
        }
        return s;
    }


    public Object deadAtEnd() {
        Object o = "";
        Object z = null;
        for(int i = 0; i < 2 ; i++) {
            z = null;
            try {
                System.out.println(i);
            } catch (NullPointerException  e) {
                z = e;
            } catch (IllegalStateException  e) {
                z = e;
            }
            System.out.println(z);
        }

        return o;
    }


    public Object deadOnTrueBranch(Object o) {
        Object v = o;
        if (v.hashCode() == 101212) {
            v = new Object(); // original v is dead...
            v.hashCode();
            return v;
        } else {
            return v;
        }
    }

    public Object deadOnFalseBranch(Object o) {
        Object v = o;
        if (v.hashCode() == 101212) {
            return v; // here, V ist NOT DEAD when we reach the return instruction!
        } else {
            v = new Object(); // original v is dead...
            v.hashCode();
            return v;
        }
    }

    public Object singleTargetAThrow() { // this is a variant of loopWithBreak
        ControlFlowException theCFE = new ControlFlowException();
        Object o = new Object();
        try {
            for (int i = 0; true; i++) {
                this.someFieldA = o;
                o = this.someFieldB;
                if (someInt < i) {
                    o = "useless"; // never used
                    throw theCFE; // this is "just a goto"...
                }
            }
        } catch (ControlFlowException cfe) {
            o = "kill it";
            return o;
        }
    }

    public Object loopWithBreak() { // this is a variant of singleTargetAThrow
        Object o = new Object();
        for (int i = 0; true; i++) {
            this.someFieldA = o;
            o = this.someFieldB;
            if (someInt < i) {
                o = "useless"; // never used
                break;
            }
        }
        o = "kill it";
        return o;
    }

    public Object nestedIrrelevantControlFlow() {
        Object o = new Object();
        for (int i = 0; true; i++) {
            processIt(o);
            o = this.someFieldB; // never used (DEAD)
            if (i % 3 == 0) {
                processIt("irrelevant");
            } else {
                processIt("also irrelevant");
            }
            if (someInt < i) {
                o = "useless"; // never used (DEAD)
                break;
            }
            o = "relevant";
        }
        o = "kill it";
        return o;
    }
}
