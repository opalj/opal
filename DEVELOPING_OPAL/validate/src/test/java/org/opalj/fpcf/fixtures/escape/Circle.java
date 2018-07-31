/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

public class Circle {
    public static int pi = 3;
    public int radius;
    public int area;

    public Circle(int radius) {
        this.radius = radius;
        this.area = pi * radius * radius;
    }

    public Circle() {
        this(1);
    }

    @Override
    public String toString() {
        return "r: "+radius;
    }

    public int getArea() {
        return area;
    }
}
