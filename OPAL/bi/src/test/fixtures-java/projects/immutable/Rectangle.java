/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package immutable;

/**
 * This class was used to create a class file with some well defined issues. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 * 
 * @author Michael Eichberg
 */
public class Rectangle {

    private Point ul;

    private Point lr;

    public Rectangle(Point ul, Point lr) {
        this.ul = ul;
        this.lr = lr;
    }

    public Point getLr() {
        return lr;
    }

    public Point getUl() {
        return ul;
    }

}
