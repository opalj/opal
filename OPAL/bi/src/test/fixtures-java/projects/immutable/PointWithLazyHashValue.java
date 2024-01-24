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
public class PointWithLazyHashValue {

    public final int x;

    public final int y;

    public PointWithLazyHashValue(int x, int y) {
        this.x = x;
        this.y = y;
    }

    //
    // NOT THREAD-SAFE!
    //

    private int hashValue = 0;

    @Override
    public int hashCode() {
        // return cached value
        if (hashValue != 0)
            return hashValue;

        // create hash value
        hashValue = x << 16 | y;
        if (hashValue == 0)
            hashValue = -1;
        return hashValue;
    }
}
