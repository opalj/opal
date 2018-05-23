package org.opalj.fpcf.fixtures.declared_methods.base;

import org.opalj.fpcf.properties.declared_methods.DeclaredMethod;

/**
 * Two interfaces declaring the same method.
 *
 * @author Dominik Helm
 */

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V", declaringClass = AnInterface.class)
public interface AnInterface {
    void interfaceMethod();
}

@DeclaredMethod(name = "interfaceMethod", descriptor = "()V",
        declaringClass = AnotherInterface.class)
interface AnotherInterface {
    void interfaceMethod();
}
