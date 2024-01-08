/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.highPrecision;

import org.opalj.ai.test.invokedynamic.annotations.InvokedMethod;
import org.opalj.ai.test.invokedynamic.annotations.InvokedMethods;
import static org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm.CHA;
import static org.opalj.ai.test.invokedynamic.annotations.CallGraphAlgorithm.BasicVTA;

;

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
 * 
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE).
 * 
 * 
 * -->
 * 
 * @author Michael Reif
 */
public class ExecutionClass {

    private IBase innerClass = new InnerClass();

    class InnerClass implements IBase {

        public IBase interfaceMethod() {
            return this;
        }
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/highPrecision/ExecutionClass$InnerClass", name = "interfaceMethod", line = 76),
            @InvokedMethod(receiverType = "callgraph/highPrecision/ConcreteClass", name = "interfaceMethod", line = 76, isContainedIn = {
                    CHA, BasicVTA }) })
    public void testInnerClass() {
        innerClass.interfaceMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/highPrecision/ExecutionClass$1", name = "interfaceMethod", line = 92),
            @InvokedMethod(receiverType = "callgraph/highPrecision/ConcreteClass", name = "interfaceMethod", line = 92, isContainedIn = { CHA }),
            @InvokedMethod(receiverType = "callgraph/highPrecision/ExecutionClass$InnerClass", name = "interfaceMethod", line = 92, isContainedIn = { CHA }) })
    public void testAnonClass() {
        IBase anon = new IBase() {

            @Override
            public IBase interfaceMethod() {
                return this;
            }

        };
        anon.interfaceMethod();
    }
}
