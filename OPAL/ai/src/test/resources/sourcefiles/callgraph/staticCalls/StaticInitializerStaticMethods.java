/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package callgraph.staticCalls;

import callgraph.base.AbstractBase;
import callgraph.base.ConcreteBase;
import callgraph.base.SimpleBase;

/**
 * This class was used to create a class file with some well defined attributes. The
 * created class is subsequently used by several tests.
 * 
 * NOTE<br />
 * This class is not meant to be (automatically) recompiled; it just serves documentation
 * purposes.
 * 
 * @author Marco Jacobasch
 */
public class StaticInitializerStaticMethods {

    static {
        SimpleBase.staticMethod();
        AbstractBase.staticMethod();
        ConcreteBase.staticMethod();
    }

}
