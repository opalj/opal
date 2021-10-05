/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

/**
 * Mixed in by domains that support the conversation of a Java Object into a `DomainValue`.
 *
 * @see [[AsJavaObject]] for further information on limitations.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait AsDomainValue { domain: ReferenceValuesDomain =>

    /**
     * Converts the given Java object to a corresponding `DomainValue`. The conversion may be lossy.
     *
     * @note   To convert primitive values to `DomainValue`s use the domain's
     *         respective factory methods. I.e., this method deliberately does not perform any
     *         (Un-)Boxing as it does not have the necessary information. For more
     *         information study the implementation of the [[l1.ReflectiveInvoker]].
     *
     * @param  pc The program counter of the instruction that was responsible for
     *         creating the respective value. (This is in – in general – not the
     *         instruction where the transformation is performed.)
     * @param  value The object.
     *
     * @return A `DomainReferenceValue`.
     */
    def toDomainValue(pc: Int, value: Object): DomainReferenceValue
}
