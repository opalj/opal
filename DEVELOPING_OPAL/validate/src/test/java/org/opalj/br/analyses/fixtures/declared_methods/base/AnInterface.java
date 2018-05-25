package org.opalj.br.analyses.fixtures.declared_methods.base;

import org.opalj.br.analyses.properties.declared_methods.DeclaredMethod;

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
