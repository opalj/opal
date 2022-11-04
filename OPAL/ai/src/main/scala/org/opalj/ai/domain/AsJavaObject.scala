/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Mixed in by domains that support the conversation of a `DomainValue` into
 * a respective Java object. This Java object can then be used to perform method
 * invocations.
 *
 * ==Limitation==
 * Using `AsJavaObject` will only work reasonably iff the respective class
 * is either in the classpath of the JVM or a class loader (initialized with the
 * project's classpath) is used.
 * The latter, however, does not work for classes on the bootclasspath (e.g.,
 * `java.lang.String`). In that case it is necessary to check that the code of the
 * analyzed application is compatible with the one on the class path.
 * '''To avoid accidental imprecision in the analysis you should use this features
 * only for stable classes belonging to the core JDK (`java.lang...`.)'''
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait AsJavaObject { domain: ReferenceValuesDomain =>

    /**
     * Converts – if possible – a given `DomainValue` to a Java object that is
     * appropriately initialized.
     *
     * ==Implementation==
     * Every domain that supports the creation of a Java object's based on a domain
     * value is expected to implement this method and to test if it can create
     * a precise representation of the given value. If not, the implementation has to delegate
     * the responsibility to the super method to creat an abstract representation.
     * {{{
     * abstract override def toJavaObject(value : DomainValue): Option[Object] = {
     *  if(value...)
     *      // create and return Java object
     *  else
     *      super.toJavaObject(value)
     * }
     * }}}
     *
     * @note   This operation is generally only possible if the domain value maintains
     *         ''enough'' state information to completely initialize the Java object.
     *
     * @return Some(Object) is returned if it was possible to create a compatible
     *         corresponding Java object; otherwise `None` is returned.
     *         Default: `None` unless the `value` is null. In the latter case `Some(null)`
     *         is returned.
     */
    def toJavaObject(pc: Int, value: DomainValue): Option[Object] = None

}
