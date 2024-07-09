/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package simpleCallgraph;

import org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm;
import org.opalj.ai.test.invokedynamic.annotations.InvokedConstructor;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

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
 * 
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class B implements Base {

    String b = "B";

    @Override
    public String callOnInstanceField() {
        return b;
    }

    @Override
    @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 71)
    @InvokedConstructor(receiverType = "simpleCallgraph/A", line = 71)
    public void callOnConstructor() {
        new A().callOnConstructor();
    }

    @Override
    @InvokedMethods({
            @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 79),
            @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 79)})
    public void callOnMethodParameter(@SuppressWarnings("hiding") Base b) {
        b.callOnConstructor();
    }
}
