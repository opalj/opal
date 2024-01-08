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
 * -->
 * 
 * @author Marco Jacobasch
 * @author Michael Reif
 */
public class A implements Base {

    Base b = new B();

    @Override
    @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnInstanceField", line = 65)
    public String callOnInstanceField() {
        return b.callOnInstanceField();
    }

    @Override
    @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 72)
    @InvokedConstructor(receiverType = "simpleCallgraph/B", line = 72)
    public void callOnConstructor() {
        new B().callOnConstructor();
    }

    @Override
    @InvokedMethods({
    	@InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnMethodParameter", line = 82),
            @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 83),
            @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 83)})
    public void callOnMethodParameter(Base b) {
        if (b != null) {
            this.callOnMethodParameter(null);
            b.callOnConstructor();
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "simpleCallgraph/A", name = "callOnConstructor", line = 91),
            @InvokedMethod(receiverType = "simpleCallgraph/B", name = "callOnConstructor", line = 92)})
    public void directCallOnConstructor() {
        new A().callOnConstructor();
        new B().callOnConstructor();
    }

}
