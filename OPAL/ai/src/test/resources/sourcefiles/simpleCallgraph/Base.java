/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package simpleCallgraph;

/**
 * This class was used to create a class file with some well defined properties. The
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
 * @author Marco Jacobasch
 *
 */
public interface Base {

    public String callOnInstanceField();

    public void callOnConstructor();

    public void callOnMethodParameter(Base b);

}
