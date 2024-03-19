/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.virtualCalls;

import callgraph.base.AbstractBase;
import callgraph.base.AlternateBase;
import callgraph.base.Base;
import callgraph.base.ConcreteBase;
import callgraph.base.SimpleBase;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;

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
public class CallByParameter {

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "interfaceMethod", line = 68),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 68) })
    void callByInterface(Base object) {
        object.interfaceMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 73)
    void callByInterface(AbstractBase object) {
        object.interfaceMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 78)
    void callByInterface(ConcreteBase object) {
        object.interfaceMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 83)
    void callByInterface(AlternateBase object) {
        object.interfaceMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "interfaceMethod", line = 88)
    void callByInterface(SimpleBase object) {
        object.interfaceMethod();
    }

}
