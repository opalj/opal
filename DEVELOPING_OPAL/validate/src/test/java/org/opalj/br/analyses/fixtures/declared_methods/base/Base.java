/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.fixtures.declared_methods.base;

import org.opalj.br.analyses.fixtures.declared_methods.sub.Sub;
import org.opalj.br.analyses.properties.declared_methods.DeclaredMethod;

/**
 * Classes annotated with the DefinedMethods that correspond to them.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod1", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod2", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = Base.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = Base.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = Base.class)
public abstract class Base {
    public abstract void abstractMethod();

    void packagePrivateMethod1() { }

    void packagePrivateMethod2() { }

    public void overloadedMethod(int a) { }

    public void overloadedMethod(float a) { }

    public void interfaceMethod() { }
}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = SubSub.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = SubSub.class)
@DeclaredMethod(name = "packagePrivateMethod1", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod1", descriptor = "()V", declaringClass = Sub.class)
@DeclaredMethod(name = "packagePrivateMethod2", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "packagePrivateMethod2", descriptor = "()V", declaringClass = Sub.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = Base.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = Base.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = Base.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
class SubSub extends Sub {
    // This implementation must be used instead of the abstract one from Base
    public void abstractMethod() { }
}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = ImplementsInterfaces.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V",
        declaringClass = { AnInterface.class, AnotherInterface.class })
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
abstract class ImplementsInterfaces implements AnInterface, AnotherInterface {
    // Implements two interfaces with "interfaceMethod", both of which are maximally specific!

}

@DeclaredMethod(name = "<init>", descriptor = "()V",
        declaringClass = ImplementsInterfaceMethod.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V",
        declaringClass = ImplementsInterfaceMethod.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
class ImplementsInterfaceMethod extends ImplementsInterfaces {
    // This method must be used instead of any of the methods from the implemented interfaces
    public void interfaceMethod() { }
}