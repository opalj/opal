/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with some non-trivial method calls.
 *
 * @author Michael Eichberg
 *
 */
public class MethodCalls {

    public Object barVarargs(Object... os) {
        return os;
    }

    public void fooVarargs() {
        barVarargs(new X(), new Y());
    }

    public Object bar(X x, Y y) {
        return x.toString() + y.toString();
    }

    public void foo() {
        bar(new X(), new Y());
    }
}

class X {}

class Y {}
