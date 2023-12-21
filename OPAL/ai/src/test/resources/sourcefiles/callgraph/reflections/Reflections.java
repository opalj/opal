/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import callgraph.base.Base;
import callgraph.base.ConcreteBase;
import callgraph.base.SimpleBase;

import org.opalj.ai.test.invokedynamic.annotations.InvokedConstructor;
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
public class Reflections {

    @InvokedConstructor(receiverType = "callgraph/base/ConcreteBase", line = 69)
    @InvokedMethod(receiverType = "callgraph/base/ConcreteBase", name = "implementedMethod", line = 69)
    void callAfterCastingToInterface() {
        ((Base) new ConcreteBase()).implementedMethod();
    }

    @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 76, isReflective = true)
    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 77)
    void callInstantiatedByReflection() throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        Base instance = (Base) Class.forName("fixture.SimpleBase").newInstance();
        instance.implementedMethod();
    }

    @InvokedConstructor(receiverType = "callgraph/base/SimpleBase", line = 84)
    @InvokedMethod(receiverType = "callgraph/base/SimpleBase", name = "implementedMethod", line = 86, isReflective = true)
    void callMethodByReflection() throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Base base = new SimpleBase();
        Method method = base.getClass().getDeclaredMethod("implementedMethod");
        method.invoke(base);
    }

    // TODO combine string and call via reflection

}
