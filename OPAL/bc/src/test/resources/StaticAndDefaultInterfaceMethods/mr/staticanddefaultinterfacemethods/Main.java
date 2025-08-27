/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mr.staticanddefaultinterfacemethods;

class C implements SubIntf {}

class Helper {
    public static void println(java.lang.String s) {
        System.out.println(s);
    }
}

public class Main {
    public static void main(String[] args) {
        run(new C());
    }

    public static void run(SubIntf c) {
        // This invokes the default method from SuperIntf
        c.m();
    }

}
