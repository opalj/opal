/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mr.maximallyspecificinterfacemethods;

class C implements Intf {
    public void f(){ this.m(); }
}

class Helper {
    public static void println(java.lang.String s) {
        System.out.println(s);
    }
}

public class Main {
    public static void main(String[] args) {
        C c  = new C();
        c.f();
    }
}
