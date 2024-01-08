/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.base;

import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;

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
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class AlternateBase extends AbstractBase {

    @SuppressWarnings("hiding")
    public String string;
    public double number;

    public AlternateBase() {
        this("alternate");
    }

    public AlternateBase(String s) {
        this(s, 0);
    }

    public AlternateBase(String s, double d) {
        this.string = s;
        this.number = d;
    }

    @Override
    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "abstractImplementedMethod", line = 78)
    public void abstractMethod() {
        super.abstractImplementedMethod();
    }

    @Override
    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "implementedMethod", line = 84)
    public void implementedMethod() {
        super.implementedMethod();
    }

}
