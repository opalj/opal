/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import org.opalj.br.ComputationalType
import org.opalj.br.ComputationalTypeReturnAddress
import org.opalj.ai.ValueOrigin
import org.opalj.ai.isImmediateVMException
import org.opalj.ai.isMethodExternalExceptionOrigin
import org.opalj.ai.pcOfImmediateVMException
import org.opalj.ai.pcOfMethodExternalException

/**
 * Identifies a variable which has a single static definition/initialization site.
 */
abstract class DUVar[+Value <: ValueInformation] extends Var[DUVar[Value]] {

    /**
     * The information about the variable that were derived by the underlying data-flow analysis.
     */
    def value: Value

    final def cTpe: ComputationalType = value.computationalType

    /**
     * The indexes of the instructions which use this variable.
     *
     * '''Defined, if and only if this is an assignment statement.'''
     */
    def usedBy: IntTrieSet

    /**
     * The indexes of the instructions which initialize this variable/
     * the origin of the value identifies the expression which initialized this variable.
     *
     * '''Defined, if and only if this is a variable usage.'''
     *
     * In general, the origin is positive and identifies a single, unique assignment statement.
     * However, the origin can  be negative if the value assigned to the variable is not
     * directly created by the program, but is either created by the JVM (e.g., the
     * `DivisionByZeroException` created by the JVM when an `int` value is divided by `0`) or
     * is just a constant.
     */
    def definedBy: IntTrieSet

    override def toCanonicalForm(
        implicit
        ev: DUVar[Value] <:< DUVar[ValueInformation]
    ): DUVar[ValueInformation]

}

object DUVar {

    @volatile var printDomainValue: Boolean = false

}

/**
 * Extractor to get the definition site of an expression's/statement's value.
 *
 * This extractor may fail (i.e., throw an exception), when the expr is not a [[DVar]] or
 * a [[Const]]; this decision was made to capture programming failures as early as possible
 * ([[https://www.opal-project.de/TAC.html flat]]).
 *
 * @example
 *          To get a return value's definition sites (unless the value is constant).
 *          {{{
 * val tac.ReturnValue(pc,tac.DefSites(defSites)) = code.stmts(5)
 *          }}}
 */
object DefSites {

    /**
     * Defines an extractor to get the definition site of an expression's/statement's value.
     * Returns the empty set if the value is a constant.
     */
    def unapply(valueExpr: Expr[DUVar[_]] /*Expr to make it fail!*/ ): Some[IntTrieSet] = {
        Some(
            valueExpr match {
                case UVar(_, defSites) => defSites
                case _: Const          => IntTrieSet.empty
            }
        )
    }

    def toString(defSites: IntTrieSet): Iterator[String] = {
        defSites.iterator.map { defSite =>
            if (isImmediateVMException(defSite))
                "exception[VM]@"+pcOfImmediateVMException(defSite)
            else if (isMethodExternalExceptionOrigin(defSite))
                "exception@"+pcOfMethodExternalException(defSite)
            else if (defSite < 0) {
                "param"+(-defSite - 1).toHexString
            } else {
                "lv"+defSite.toHexString
            }
        }
    }

}

/**
 * A (final) variable definition, which is uniquely identified by its origin/the index of
 * the corresponding AssignmentStatement.
 * I.e., per method there must be at most one D variable which
 * has the given origin. Initially, the pc of the underlying bytecode instruction is used.
 *
 * @param value The value information.
 *
 */
class DVar[+Value <: ValueInformation /*org.opalj.ai.ValuesDomain#DomainValue*/ ] private (
        private[tac] var origin:   ValueOrigin,
        val value:                 Value,
        private[tac] var useSites: IntTrieSet
) extends DUVar[Value] {

    assert(origin >= 0)

    def copy[V >: Value <: ValueInformation /*org.opalj.ai.ValuesDomain#DomainValue*/ ](
        origin:   ValueOrigin = this.origin,
        value:    V           = this.value,
        useSites: IntTrieSet  = this.useSites
    ): DVar[V] = {
        new DVar(origin, value, useSites)
    }

    def definedBy: Nothing = throw new UnsupportedOperationException

    /**
     * The set of the indexes of the statements where this `variable` is used. Hence, a use-site
     * is always positive.
     */
    def usedBy: IntTrieSet = useSites

    def name: String = {
        val n = s"lv${origin.toHexString}"
        if (DUVar.printDomainValue) s"$n/*domainValue=$value*/" else n
    }

    final def isSideEffectFree: Boolean = true

    /**
     * @inheritdoc
     *
     * DVars additionally remap self-uses (which don't make sense, but can be a result
     * of the transformation of exception handlers) to uses of the next statement.
     */
    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        assert(
            origin >= 0,
            s"DVars are not intended to be used to model parameters/exceptions (origin=$origin)"
        )
        val initialNewOrigin = pcToIndex(origin)
        val newOrigin =
            if (isIndexOfCaughtExceptionStmt(initialNewOrigin))
                initialNewOrigin + 1
            else
                initialNewOrigin
        origin = newOrigin
        useSites = useSites map { useSite =>
            // a use site is always positive...
            val newUseSite = pcToIndex(useSite)
            if (newUseSite == newOrigin)
                newUseSite + 1
            else
                newUseSite
        }
    }

    override def toCanonicalForm(
        implicit
        ev: DUVar[Value] <:< DUVar[ValueInformation]
    ): DVar[ValueInformation] = {
        new DVar(origin, value.toCanonicalForm, useSites)
    }

    override def hashCode(): Int = Var.ASTID * 1171 - 13 + origin

    override def toString: String = {
        s"DVar(useSites=${useSites.mkString("{", ",", "}")},value=$value,origin=$origin)"
    }

}

object DVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        origin: ValueOrigin, value: d.DomainValue, useSites: IntTrieSet
    ): DVar[d.DomainValue] = {

        assert(useSites != null, s"no uses (null) for $origin: $value")
        assert(value != null)
        assert(
            value == d.TheIllegalValue || value.computationalType != ComputationalTypeReturnAddress,
            s"value has unexpected computational type: $value"
        )

        new DVar[d.DomainValue](origin, value, useSites)
    }

    def unapply[Value <: ValueInformation /* org.opalj.ai.ValuesDomain#DomainValue*/ ](
        d: DVar[Value]
    ): Some[(Value, IntTrieSet)] = {
        Some((d.value, d.useSites))
    }

}

class UVar[+Value <: ValueInformation /*org.opalj.ai.ValuesDomain#DomainValue*/ ] private (
        val value:                 Value,
        private[tac] var defSites: IntTrieSet
) extends DUVar[Value] {

    def name: String = {
        DefSites.toString(defSites).mkString(
            "{",
            ", ",
            if (DUVar.printDomainValue) s"}/*domainValue=$value*/" else "}"
        )
    }

    def definedBy: IntTrieSet = defSites

    def usedBy: Nothing = throw new UnsupportedOperationException

    final def isSideEffectFree: Boolean = true

    private[tac] override def remapIndexes(
        pcToIndex:                    Array[Int],
        isIndexOfCaughtExceptionStmt: Int => Boolean
    ): Unit = {
        defSites = defSites map { defSite =>
            if (defSite >= 0) {
                val defSiteIndex = pcToIndex(defSite)
                if (isIndexOfCaughtExceptionStmt(defSiteIndex))
                    defSiteIndex + 1 // we have to skip the "CaughtExceptionStatement" - it can't be a definition site!
                else
                    defSiteIndex
            } else if (ai.isImplicitOrExternalException(defSite))
                ai.remapPC(pcToIndex)(defSite)
            else
                defSite /* <= it is referencing a parameter */
        }
    }

    override def toCanonicalForm(
        implicit
        ev: DUVar[Value] <:< DUVar[ValueInformation]
    ): UVar[ValueInformation] = {
        new UVar(value.toCanonicalForm, defSites)
    }

    override def hashCode(): Int = Var.ASTID * 1171 - 113 + defSites.hashCode

    override def equals(other: Any): Boolean = {
        other match {
            case that: UVar[_] => this.defSites == that.defSites
            case _             => false
        }
    }

    override def toString: String = {
        s"UVar(defSites=${defSites.mkString("{", ",", "}")},value=$value)"
    }

}

object UVar {

    def apply(
        d: org.opalj.ai.ValuesDomain
    )(
        value: d.DomainValue, defSites: IntTrieSet
    ): UVar[d.DomainValue] = {
        new UVar[d.DomainValue](value, defSites)
    }

    def apply(value: ValueInformation, defSites: IntTrieSet): UVar[ValueInformation] = {
        new UVar(value, defSites)
    }

    def unapply[Value <: ValueInformation /* org.opalj.ai.ValuesDomain#DomainValue*/ ](
        u: UVar[Value]
    ): Some[(Value, IntTrieSet)] = {
        Some((u.value, u.defSites))
    }

}
