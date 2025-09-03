/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mr.inheritedstaticinterfacemethods;

class Helper {
    public static void println(java.lang.String s) {
        System.out.println(s);
    }
}

class X {
    static void m(){ Helper.println("X.m"); }
}

class SubX extends X { }

interface I {
    static void m() { Helper.println("Intf.m"); }
}

interface SubI extends I { }
