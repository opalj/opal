/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.staticCalls;

import callgraph.base.AbstractBase;
import callgraph.base.ConcreteBase;
import callgraph.base.SimpleBase;
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
public class CallStaticMethods {

    @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "staticMethod", isStatic = true, line = 63)
    void callStaticAbstract() {
        AbstractBase.staticMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/ConcreteBase", name = "staticMethod", isStatic = true, line = 68)
    void callStaticConcrete() {
        ConcreteBase.staticMethod();
    }

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "staticMethod", isStatic = true, line = 73)
    void callStaticSimple() {
        SimpleBase.staticMethod();
    }

}
