/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.constructors;

import callgraph.base.AbstractBase;
import callgraph.base.AlternateBase;
import callgraph.base.Base;
import callgraph.base.ConcreteBase;
import callgraph.base.SimpleBase;
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
 * 
 * -->
 * 
 * @author Marco Jacobasch
 */
public class DefaultConstructor {

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 66)
    public void createSimpleBase() {
        Base simpleBase = new SimpleBase();
    }

    class MyBase extends AbstractBase {

        @Override
        public void abstractMethod() {/* empty */
        }
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/constructors/DefaultConstructor$MyBase", line = 79)
    public void createAbstractBase() {
        Base abstractBase = new MyBase();
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/ConcreteBase", line = 85)
    public void createConcreteBase() {
        Base concreteBase = new ConcreteBase();
    }

    @SuppressWarnings("unused")
    @InvokedConstructor(receiverType = "callgraph/base/AlternateBase", line = 91)
    public void createAlternateBase() {
        Base alternerateBase = new AlternateBase();
    }
}
