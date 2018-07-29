/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.virtualCalls;

import callgraph.base.AbstractBase;
import callgraph.base.AlternateBase;
import callgraph.base.Base;
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
public class CallHashcodeOnInterface {

    Base simpleBase = new SimpleBase();
    Base concreteBase = new ConcreteBase();
    Base alternerateBase = new AlternateBase();

    Base abstractBase = new AbstractBase() {
        @Override
        public void abstractMethod() {
            // empty
        }
    };

    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "hashCode", returnType = Integer.class, line = 76)
    void callHashCodeOnSimpleBase() {
        simpleBase.hashCode();
    }

    @InvokedMethod(receiverType = "java/lang/Object", name = "hashCode", returnType = Integer.class, line = 81)
    void callHashCodeOnConcreteBase() {
        concreteBase.hashCode();
    }

    @InvokedMethod(receiverType = "java/lang/Object", name = "hashCode", returnType = Integer.class, line = 86)
    void callHashCodeOnAlternateBase() {
        alternerateBase.hashCode();
    }

    @InvokedMethod(receiverType = "java/lang/Object", name = "hashCode", returnType = Integer.class, line = 91)
    void callHashCodeOnAbstractBase() {
        abstractBase.hashCode();
    }
}
