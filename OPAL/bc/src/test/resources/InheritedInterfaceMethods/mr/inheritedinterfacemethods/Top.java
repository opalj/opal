/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package mr.inheritedinterfacemethods;

public class Top {
    protected void m() {
        System.out.println("Top.m");
    }

    public static void main(String[] args) {
        new Sub().m();
    }
}
