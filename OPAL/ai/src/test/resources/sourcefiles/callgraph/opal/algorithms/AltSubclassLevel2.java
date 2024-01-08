/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.opal.algorithms;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * <!--
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * 
 * @author Michael Reif
 */

public class AltSubclassLevel2 extends SubclassLevel1 {

    @Override
    public void implementedInEachSubclass() {
        this.toString();
    }
}
