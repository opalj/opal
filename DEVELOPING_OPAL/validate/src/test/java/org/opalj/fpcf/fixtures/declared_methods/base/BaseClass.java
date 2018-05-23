package org.opalj.fpcf.fixtures.declared_methods.base;

import org.opalj.fpcf.fixtures.declared_methods.sub.SubClass;
import org.opalj.fpcf.properties.declared_methods.DeclaredMethod;

/**
 * Classes annotated with the DefinedMethods that correspond to them.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "packagePrivateMethod", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = BaseClass.class)
public abstract class BaseClass {
    public abstract void abstractMethod();

    void packagePrivateMethod() { }

    public void overloadedMethod(int a) { }

    public void overloadedMethod(float a) { }

    public void interfaceMethod() { }
}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = SubSubClass.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = SubSubClass.class)
@DeclaredMethod(name = "packagePrivateMethod", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "packagePrivateMethod", descriptor = "()V", declaringClass = SubClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = BaseClass.class)
class SubSubClass extends SubClass {
    // This implementation must be used instead of the abstract one from BaseClass
    public void abstractMethod() { }
}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = ImplementsInterfaces.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
abstract class ImplementsInterfaces implements AnInterface, AnotherInterface {
    // Implements two interfaces with the same method, only one of them may be used!
}

@DeclaredMethod(name = "<init>", descriptor = "()V",
        declaringClass = ImplementsInterfaceMethod.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V",
        declaringClass = ImplementsInterfaceMethod.class)
class ImplementsInterfaceMethod extends ImplementsInterfaces {
    // This method must be used instead of any of the methods from the implemented interfaces
    public void interfaceMethod() { }
}