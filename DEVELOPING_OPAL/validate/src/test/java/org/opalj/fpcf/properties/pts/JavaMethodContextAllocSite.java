/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.pts;

/**
 * Describes an allocation site in a java method
 */
public @interface JavaMethodContextAllocSite {
    // alloc site context class
    Class<?> cf();
    // alloc site context method, jvm bytecode subsignature such as "main([Ljava/lang/String])V"
    String methodName();
    String methodDescriptor();

    /**
     * alloc site line number
     */
    int allocSiteLinenumber();

    /**
     * alloc site type
     * Must be given in JVM binary notation (e.g. Ljava/lang/Object;)
     */
    String allocatedType();


}
