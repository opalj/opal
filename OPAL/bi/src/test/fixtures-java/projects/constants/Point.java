/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package constants;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */

public class Point {

    static final Point Origin = new Point(0, 0);

    static final int e2 = (int) Math.pow(Math.E, 2);

    static final int eX = e2 * 2;

    static final int eZ = e2 * eX;

    public final int x;

    public final int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

}
