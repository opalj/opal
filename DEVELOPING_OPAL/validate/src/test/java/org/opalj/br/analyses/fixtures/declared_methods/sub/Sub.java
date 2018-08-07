/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.fixtures.declared_methods.sub;

import org.opalj.br.analyses.fixtures.declared_methods.base.Base;
import org.opalj.br.analyses.fixtures.declared_methods.base.AnInterface;
import org.opalj.br.analyses.properties.declared_methods.DeclaredMethod;

/**
 * A subclass in a different package that its super class with a package private method shadowing
 * the base one.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = Sub.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod1", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod1", descriptor = "()V", declaringClass = Sub.class)
@DeclaredMethod(name = "packagePrivateMethod2", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod2", descriptor = "()V", declaringClass = Sub.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = Base.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = Base.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
public abstract class Sub extends Base implements AnInterface {
    // This class has two "packagePrivateMethod1"s, this one and the one from Base!
    void packagePrivateMethod1() { }

    // This method is public, but does not override packagePrivateMethod2 from Base!
    public void packagePrivateMethod2() { }
}
