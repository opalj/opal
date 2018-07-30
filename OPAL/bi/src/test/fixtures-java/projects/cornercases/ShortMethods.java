/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package cornercases;

/**
 * This class was used to create a class file with some well defined properties. The
 * created class is subsequently used by several tests.
 *
 * NOTE<br />
 * This class is only meant to be (automatically) compiled by OPAL's build script.
 *
 * @author Michael Eichberg
 */
public class ShortMethods {

	public static void staticMethodWhichJustThrowsAnException (Exception e) throws Exception {
        throw e;
    }

    public void instanceMethodWhichJustThrowsAnException (Exception e) throws Exception {
        throw e;
    }

    public void endlessWhileLoop (Exception e) {
        while (true) ;
    }

    public void endlessForLoop (Exception e) {
        for(;true;) {;}
    }

    public void justReturns (Exception e) {
        return;
    }

    public int justReturnsGivenInt (int e) {
        return e;
    }

    public double justReturnsGivenDouble (double e) {
        return e;
    }
}
