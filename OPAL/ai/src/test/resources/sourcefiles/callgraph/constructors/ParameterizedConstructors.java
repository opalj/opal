/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.constructors;

import callgraph.base.AlternateBase;
import callgraph.base.Base;
import callgraph.base.ConcreteBase;
import org.opalj.ai.test.invokedynamic.annotations.InvokedConstructor;

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
 * -->
 * 
 * @author Marco Jacobasch
 */
public class ParameterizedConstructors {

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/ConcreteBase", parameterTypes = { String.class }, line = 64)
    public void createConcreteBaseSingleParameter() {
        Base concreteBase = new ConcreteBase("test");
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/AlternateBase", parameterTypes = { String.class }, line = 70)
    public void createAlternateBaseSingleParameter() {
        Base alternerateBase = new AlternateBase("test");
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/ConcreteBase", parameterTypes = {
            String.class, Integer.class }, line = 77)
    public void createConcreteBaseTwoParameters() {
        Base concreteBase = new ConcreteBase("test", 42);
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/AlternateBase", parameterTypes = {
            String.class, Double.class }, line = 84)
    public void createAlternateBaseTwoParameters() {
        Base alternerateBase = new AlternateBase("test", 42);
    }
}
