package org.opalj.fpcf.fixtures.declared_methods.sub;

import org.opalj.fpcf.fixtures.declared_methods.base.BaseClass;
import org.opalj.fpcf.fixtures.declared_methods.base.AnInterface;
import org.opalj.fpcf.properties.declared_methods.DeclaredMethod;

/**
 * A subclass in a different package that its super class with a package private method shadowing
 * the base one.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = SubClass.class)
@DeclaredMethod(name = "abstractMethod", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "packagePrivateMethod", descriptor = "()V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "packagePrivateMethod", descriptor = "()V", declaringClass = SubClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(I)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "overloadedMethod", descriptor = "(F)V", declaringClass = BaseClass.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = BaseClass.class)
public abstract class SubClass extends BaseClass implements AnInterface {
    // This class has two package private methods, this one and the one from BaseClass!
    void packagePrivateMethod() { }
}
