/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.fixtures.declared_methods.base;

import org.opalj.br.analyses.properties.declared_methods.DeclaredMethod;

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = BaseInterface.class)
interface BaseInterface {
    default void interfaceMethod() { }
}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
interface SubInterface extends BaseInterface {
    void interfaceMethod();
}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = ImplementsBoth.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
abstract class ImplementsBoth implements BaseInterface, SubInterface {}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = ImplementsBoth2.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
abstract class ImplementsBoth2 implements SubInterface, BaseInterface {}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = BaseClass1.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = BaseInterface.class)
abstract class BaseClass1 implements BaseInterface {}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = SubClass1.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
abstract class SubClass1 extends BaseClass1 implements SubInterface {}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = BaseClass2.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
abstract class BaseClass2 implements SubInterface {}

@DeclaredMethod(name = "<init>", descriptor = "()V", declaringClass = SubClass2.class)
@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = SubInterface.class)
abstract class SubClass2 extends BaseClass2 implements BaseInterface {}