/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.virtualCalls;

import callgraph.base.AbstractBase;
import callgraph.base.AlternateBase;
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
public class CallConcreteObjects {

    // @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 12)
    SimpleBase simpleBase = new SimpleBase();

    // @InvokedConstructor(receiverType = "callgraph/base/ConcreteBase", line = 15)
    ConcreteBase concreteBase = new ConcreteBase();

    // @InvokedConstructor(receiverType = "callgraph/base/AlternateBase", line = 18)
    AlternateBase alternerateBase = new AlternateBase();

    // @InvokedConstructor(receiverType = "callgraph/base/AbstractBase", line = 21)
    AbstractBase abstractBase = new AbstractBase() {

        @Override
        public void abstractMethod() {
            // empty
        }
    };

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 88),
            @InvokedMethod(receiverType = "callgraph/base/ConcreteBase", name = "implementedMethod", line = 89),
            @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "implementedMethod", line = 90),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "implementedMethod", line = 91) })
    void callImplementedMethod() {
        simpleBase.implementedMethod();
        concreteBase.implementedMethod();
        alternerateBase.implementedMethod();
        abstractBase.implementedMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "abstractMethod", line = 99),
            @InvokedMethod(receiverType = "callgraph/base/ConcreteBase", name = "abstractMethod", line = 100),
            @InvokedMethod(receiverType = "callgraph/base/AlternateBase", name = "abstractMethod", line = 101) })
    void callAbstractMethod() {
        simpleBase.abstractMethod();
        concreteBase.abstractMethod();
        alternerateBase.abstractMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "abstractImplementedMethod", line = 110),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "abstractImplementedMethod", line = 111),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "abstractImplementedMethod", line = 112),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "abstractImplementedMethod", line = 113) })
    void callAbstractImplementedMethod() {
        simpleBase.abstractImplementedMethod();
        concreteBase.abstractImplementedMethod();
        alternerateBase.abstractImplementedMethod();
        abstractBase.abstractImplementedMethod();
    }

    @InvokedMethods({
            @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "interfaceMethod", line = 122),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 123),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 124),
            @InvokedMethod(receiverType = "callgraph/base/AbstractBase", name = "interfaceMethod", line = 125) })
    void callInterfaceMethod() {
        simpleBase.interfaceMethod();
        concreteBase.interfaceMethod();
        alternerateBase.interfaceMethod();
        abstractBase.interfaceMethod();
    }
}
