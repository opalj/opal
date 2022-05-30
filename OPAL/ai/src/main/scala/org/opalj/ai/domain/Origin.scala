/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ComputationalType
import org.opalj.collection.IntIterator
import org.opalj.collection.immutable.IntTrieSet1
import org.opalj.collection.immutable.EmptyIntTrieSet

/**
 * Provides information about the origin (that is, def-site) of a value iff the underlying domain
 * provides the respective information; that is, this trait only defines the public API it does
 * not provide origin information on its own.
 *
 * However, a domain that provides origin information has to do so
 * for ALL values of the respective computational type category and the information has to be
 * complete.
 *
 * ==Usage==
 * To get origin information this trait needs be implemented by a domain.
 * I.e., ''just mixing in this trait will not provide origin information about values''.
 *
 * ==Implementation==
 * This trait should be inherited from by all domains that make information about
 * the origin of a value available (see [[org.opalj.ai.domain.l1.ReferenceValues]]
 * as an example); the respective domains have to override [[providesOriginInformationFor]]
 *
 * @note A [[org.opalj.br.instructions.CHECKCAST]] must not modify `origin` information; i.e.,
 *       the origin of the value on the stack before and after the checkast (unless we have
 *       an exception) must be the same!
 * @author Michael Eichberg
 */
trait Origin { domain: ValuesDomain =>
    import Origin._

    /**
     * Implementers are expected to "override" this method and to call
     * `super.providesOriginInformationFor` to make it possible to
     * stack several domain implementations which provide origin information.
     */
    def providesOriginInformationFor(ctc: ComputationalType): Boolean = false

    implicit object SingleOriginValueOrdering extends Ordering[SingleOriginValue] {
        def compare(x: SingleOriginValue, y: SingleOriginValue): Int = x.origin - y.origin
    }

    /**
     * Returns the origin(s) of the given value if the information is available.
     *
     * @return The source(s) of the given value if the information is available.
     *      Whether the information is available depends on the concrete domains.
     *      This trait only defines a general contract how to get access to a
     *      value's origin (I.e., the origin of the instruction which created the
     *      respective value.)
     *      By default this method returns an empty `Iterable`.
     */
    def originsIterator(value: DomainValue): ValueOriginsIterator = {
        value match {
            case vo: ValueWithOriginInformation => vo.originsIterator
            case _                              => IntIterator.empty
        }
    }

    /**
     * Iterates over the origin(s) of the given value if the information is available.
     */
    def foreachOrigin(value: DomainValue, f: (ValueOrigin) => Unit): Unit = {
        value match {
            case sov: SingleOriginValue    => f(sov.origin)
            case mov: MultipleOriginsValue => mov.originsIterator.foreach(f)
            case _                         => /* nothing to do */
        }
    }

    /**
     * Returns the origin(s) of the given value if the information is available.
     */
    def origins(value: DomainValue): ValueOrigins = {
        value match {
            case vo: ValueWithOriginInformation => vo.origins
            case _                              => EmptyIntTrieSet
        }
    }

}

object Origin {

    /**
     *  Common supertrait of all domain values which provide comprehensive origin information.
     */
    trait ValueWithOriginInformation {
        def originsIterator: ValueOriginsIterator
        def origins: ValueOrigins
    }

    /**
     * Should be mixed in by `DomainValue`s that have a single origin.
     */
    trait SingleOriginValue extends ValueWithOriginInformation {

        /**
         * The origin of the value (or the pseudo-origin (e.g., the index of
         * the parameter) if the true origin is unknown.)
         */
        def origin: ValueOrigin

        final def originsIterator: ValueOriginsIterator = IntIterator(origin)
        final def origins: ValueOrigins = IntTrieSet1(origin)
    }

    /**
     * Marker trait which should be mixed in by `DomainValue` classes that capture information
     * about all (multiple) origins of a value.
     */
    trait MultipleOriginsValue extends ValueWithOriginInformation

    @deprecated("introduces unnecessary (un)boxing; use the domain's (foreach)origin", "OPAL 1.1.0")
    def unapply(value: SingleOriginValue): Some[Int] = Some(value.origin)
}

object OriginsIterator {
    def unapply(value: Origin.ValueWithOriginInformation): Some[ValueOriginsIterator] = {
        Some(value.originsIterator)
    }
}

object Origins {
    def unapply(value: Origin.ValueWithOriginInformation): Some[ValueOrigins] = {
        Some(value.origins)
    }
}
