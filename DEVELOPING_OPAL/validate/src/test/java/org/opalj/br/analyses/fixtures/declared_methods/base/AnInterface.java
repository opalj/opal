/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.fixtures.declared_methods.base;

import org.opalj.br.analyses.properties.declared_methods.DeclaredMethod;

/**
 * Two interfaces declaring the same method.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
public interface AnInterface {
    void interfaceMethod();

    default void interfaceMethod2() { }
}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V",
        declaringClass = AnotherInterface.class)
interface AnotherInterface {
    void interfaceMethod();
}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = Extended.class)
interface Extended extends AnInterface {
    void interfaceMethod2();
}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = AnInterface.class)
interface Extended2 extends AnInterface {

}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
@DeclaredMethod(name = "interfaceMethod2", descriptor = "()V", declaringClass = Extended.class)
interface DiamondInheritance extends Extended, Extended2 {
    // interfaceMethod2 here has to be the abstract one from Extended!
}