/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.xl.js.testpts;

public class SimpleContainerClass {

    public int n = 7;

    public String s = "Test";
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public String toString() {
        return "SimpleContainerClass{" +
                "n=" + n +
                ", s='" + s + '\'' +
                '}';
    }
}
