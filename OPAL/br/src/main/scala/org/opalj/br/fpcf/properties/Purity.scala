/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import scala.annotation.switch

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ConfiguredPurityKey
import org.opalj.br.fpcf.properties.Purity.ContextuallyPureFlags
import org.opalj.br.fpcf.properties.Purity.ContextuallySideEffectFreeFlags
import org.opalj.br.fpcf.properties.Purity.ImpureFlags
import org.opalj.br.fpcf.properties.Purity.IsNonDeterministic
import org.opalj.br.fpcf.properties.Purity.ModifiesParameters
import org.opalj.br.fpcf.properties.Purity.NotCompileTimePure
import org.opalj.br.fpcf.properties.Purity.PerformsDomainSpecificOperations
import org.opalj.br.fpcf.properties.Purity.PureFlags
import org.opalj.br.fpcf.properties.Purity.SideEffectFreeFlags

sealed trait PurityPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Purity

}

/**
 * Describes the level of the purity of a [[org.opalj.br.DeclaredMethod]].
 *
 * In general, a method is pure if its result only depends on its inputs
 * and/or immutable global state and the execution of the method does not have any side effects;
 * an instance method's inputs include the current object that is the receiver of the call.
 *
 * '''The description of the purity levels is inspired by the definition found on wikipedia:'''
 *
 * [...] a function may be considered a pure function if both of the following statements about
 * the function hold:
 *  -   The function always evaluates to the same result value given the same argument value(s).
 *      The function result value cannot depend on any hidden information or state that may change
 *      while program execution proceeds or between different executions of the program, nor can it
 *      depend on any external input from I/O devices.
 *
 *      '''Hence, using true constants (e.g., Math.e) is not a problem as well as creating
 *      intermediate (mutable) data structures.
 *      More precisely, methods are pure if the values they refer to always (even across program
 *      runs) have an identical shape and the precise location in the heap is not relevant (e.g.,
 *      java.lang.Object.hashCode() and ...toString() are not pure).'''
 *
 *  -   Evaluation of the result does not cause any semantically observable side effect or output,
 *      such as mutation of mutable objects or output to I/O devices.
 *      The result value need not depend on all (or any) of the argument values. However, it must
 *      depend on nothing other than the argument values. The function may return multiple result
 *      values and these conditions must apply to all returned values for the function to be
 *      considered pure. If an argument is "call-by-reference", any parameter mutation will alter
 *      the value of the argument outside the function, which will render the function impure.
 *
 * Given the preceding specification, the purity of a method is described by the subclasses of
 * this property. In the following, the prefix of the names of the purity levels are used to
 * identify the certainty of the computation of the purity; the letters have the following meaning:
 *  - LB = Lower Bound; the method is at least &lt;PURITY_LEVEL&gt;, but can still be even more pure.
 *  - C = Conditional; i.e., the current purity level depends on the purity level of other entities
 *    (These states are primarily used by the analysis to record the analysis progress.)
 *  - D = The method is &lt;PURITY_LEVEL&GT; if certain '''Domain-specific''' (non-pure) operations
 *    are ignored.
 *
 * [[ImpureByLackOfInformation]] methods have no constraints on their behavior. They may have side effect and
 * depend on all accessible (global) state. Analyses can always return `(LB)Impure` as a safe
 * default value - even if they are not able to prove that a method is indeed impure; however,
 * in the latter case using [[ImpureByAnalysis]] is recommended as this enables subsequent analyses
 * to refine the property. There are several implementations of [[ImpureByAnalysis]] which give additional
 * reasoning why the analysis classified a method as impure.
 *
 * [[SideEffectFree]] methods may depend on all accessible (and mutable) state, but may not have
 * any side effects.
 * In single-threaded execution, this means that the object graph of the program may not
 * have changed between invocation of the method and its return, except for potentially additional
 * objects allocated by the method. For multi-threaded execution, the object graph may not change
 * due to the invocation of the method, again except allocation of new objects. Note that the object
 * graph may change during execution of the method due to other methods executing on concurrent
 * threads. The method must not have any effects (besides consumption of resources like memory and
 * processor time) on methods executing concurrently, in particular it may not acquire any locks on
 * objects that concurrent methods could also try to acquire.
 *
 * Analyses may return [[SideEffectFree]] as a safe default value if they are unable to guarantee
 * that a method is [[Pure]], even if it is. However, to return `SideEffectFree` the analysis has
 * to guarantee that the method does not have any side effects.
 *
 * [[Pure]] methods must be side effect free as described above and their result may only
 * depend on their parameters (including the receiver object) and global constants. In particular,
 * the result of a pure method must be structurally identical each time the method is invoked with
 * structurally identical parameters.
 * I.e., pure methods may depend on the aliasing relation between their
 * parameters or between their parameters and global constants. E.g., the following method is
 * pure:
 * {{{
 * def cmp(s: String) : Boolean = {
 *      // Reference(!) comparison of s with the interned string "Demo":
 *      s eq "Demo";
 * }
 * }}}
 * In multi-threaded execution, pure methods can not depend on any mutable state of their
 * parameters if that state might be mutated by concurrently executing methods.
 *
 * Analyses may return [[Pure]] only if they are able to guarantee that a method fulfills these
 * requirements.
 *
 * [[CompileTimePure]] methods additionally may only use global state that is compile-time constant
 * (i.e., deterministically initialized to the same value on every execution of the program). If
 * their return value is of a reference type, they must return the same reference each time they are
 * invoked with identical parameters.
 *
 * `ExternallySideEffectFree` and `ExternallyPure` methods are also similar to
 * [[SideEffectFree]] and [[Pure]] methods, respectively, but may modify their receiver object.
 * These properties may be used to detect changes that are confined because the receiver object is
 * under the control of the caller.
 *
 * [[ContextuallySideEffectFree]] and [[ContextuallyPure]] methods may modifiy not only their
 * receiver object but all of their parameters. Therefore, these properties can be used to detect
 * confined changes because all parameters are under the control of the caller.
 *
 * [[DSideEffectFree]] and [[DPure]] methods may perform actions that are
 * generally considered impure (or non-deterministic in the case of `DPure`), but that
 * some clients may want to treat as pure. Such actions include, e.g. logging. A `Rater` is used to
 * identify such actions and the properties contain a set of reasons assigned by the Rater.
 *
 * `DExternallySideEffectFree` and `DExternallyPure` methods are similar, but may again
 * modify their receiver, while [[DContextuallySideEffectFree]] and [[DContextuallyPure]]
 * methods may modify their parameters.
 *
 * [[ImpureByLackOfInformation]] is (also) used as the fallback value if no purity information could be
 * computed for a method (no analysis is scheduled). Conceptually, clients must treat this in the
 * same way as [[ImpureByAnalysis]] except that a future refinement may be possible in case of [[ImpureByAnalysis]].
 *
 * @author Michael Eichberg
 * @author Dominik Helm
 */
sealed abstract class Purity
    extends IndividualProperty[Purity, VirtualMethodPurity]
    with PurityPropertyMetaInformation {

    /**
     * The globally unique key of the [[Purity]] property.
     */
    final def key: PropertyKey[Purity] = Purity.key

    val flags: Int

    def isCompileTimePure: Boolean = (flags & NotCompileTimePure) == 0
    def isDeterministic: Boolean = (flags & IsNonDeterministic) == 0
    def modifiesParameters: Boolean = (flags & ModifiesParameters) != 0
    def usesDomainSpecificActions: Boolean = (flags & PerformsDomainSpecificOperations) != 0

    final def aggregatedProperty = new VirtualMethodPurity(this)

    /**
     * Combines this purity value with another one to represent the progress by a purity
     * analysis in one phase.
     * Conditional as well as unconditional values are combined to the purity level that expresses
     * a weaker purity, thereby incorporating the effect of counter-examples to a stronger purity.
     * Thus, the result of this operation is used to represent a (potentially conditional) upper
     * bound on the possible final result of the purity analysis that performs this operation.
     * If one of the combined purity values is conditional and the other is not, the result will be
     * the same as if the conditional purity value was combined with the conditional value that
     * corresponds to the unconditional value.
     */
    override def meet(other: Purity): Purity = {
        other match {
            case _: ClassifiedImpure           => other
            case _ if other.modifiesParameters => other.meet(this)
            case _ =>
                Purity(this.flags | other.flags)
        }
    }

    def withoutContextual: Purity =
        if (modifiesParameters) Purity(flags & ~ModifiesParameters) else this

    val modifiedParams: IntTrieSet = EmptyIntTrieSet
}

object Purity extends PurityPropertyMetaInformation {

    /**
     * The key associated with every purity property. The name is "Purity"; the fallback is
     * "Impure".
     */
    final val key = PropertyKey.create[DeclaredMethod, Purity](
        "Purity",
        (ps: PropertyStore, _: FallbackReason, e: Entity) => {
            e match {
                case Context(dm) =>
                    val conf = ps.context(classOf[SomeProject]).has(ConfiguredPurityKey)
                    if (conf.isDefined && conf.get.wasSet(dm)) conf.get.purity(dm)
                    else ImpureByLackOfInformation
                case x =>
                    val m = x.getClass.getSimpleName+
                        " is not an org.opalj.br.fpcf.properties.Context"
                    throw new IllegalArgumentException(m)
            }
        }
    )

    final val NotCompileTimePure = 0x1
    final val IsNonDeterministic = 0x2
    final val PerformsDomainSpecificOperations = 0x4
    final val ModifiesParameters = 0x8

    final val PureFlags = NotCompileTimePure
    final val SideEffectFreeFlags = IsNonDeterministic | PureFlags
    final val ContextuallyPureFlags = PureFlags | ModifiesParameters
    final val ContextuallySideEffectFreeFlags = SideEffectFreeFlags | ModifiesParameters
    // There is no flag for impurity as analyses have to treat [[ClassifiedImpure]] specially anyway
    final val ImpureFlags = ContextuallySideEffectFreeFlags | PerformsDomainSpecificOperations

    /**
     * Returns the purity level matching the given flags for internal use by the combine operation
     * and unconditional/withoutExternal.
     * This will not return Impure/LBImpure as they have to be handled seperately.
     */
    private def apply(flags: Int): Purity = (flags: @switch) match {
        case CompileTimePure.flags => CompileTimePure
        case Pure.flags            => Pure
        // For non-pure levels, we don't have compile-time purity anymore
        case _ => (flags | NotCompileTimePure: @switch) match {
            case SideEffectFree.flags  => SideEffectFree
            case DPure.flags           => DPure
            case DSideEffectFree.flags => DSideEffectFree
        }
    }

    def apply(name: String): Option[Purity] = name match {
        case "CompileTimePure" => Some(CompileTimePure)
        case "Pure"            => Some(Pure)
        case "SideEffectFree"  => Some(SideEffectFree)
        case "DPure"           => Some(DPure)
        case "DSideEffectFree" => Some(DSideEffectFree)
        case _ if name.startsWith("ContextuallyPure{") =>
            Some(ContextuallyPure(parseParams(name.substring(17, name.length - 1))))
        case _ if name.startsWith("ContextuallySideEffectFree{") =>
            Some(ContextuallySideEffectFree(parseParams(name.substring(27, name.length - 1))))
        case _ if name.startsWith("DContextuallyPure{") =>
            Some(DContextuallyPure(parseParams(name.substring(18, name.length - 1))))
        case _ if name.startsWith("DContextuallySideEffectFree{") =>
            Some(DContextuallySideEffectFree(parseParams(name.substring(28, name.length - 1))))
        case _ => None
    }

    def parseParams(s: String): IntTrieSet = {
        val params = s.split(',')
        var result: IntTrieSet = EmptyIntTrieSet
        for (p <- params)
            result = result + Integer.valueOf(p)
        result
    }
}

/**
 * The respective method is pure and does depend only on global state that is initialized
 * deterministically to the same value on every execution of the program.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object CompileTimePure extends Purity {
    final val flags = 0 // <=> no flag is set

    final val isRefinable = false
    override def meet(other: Purity): Purity = other
}

/**
 * The respective method is at least pure.
 *
 *  @see [[Purity]] for further details regarding the purity levels.
 */
case object Pure extends Purity {
    final val flags = PureFlags
}

/**
 * The respective method is side-effect free, i.e. it does not have side-effects but its results may
 * still be non-deterministic.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object SideEffectFree extends Purity {
    final val flags = SideEffectFreeFlags
}

/*
/**
 * The respective method may modify its receiver, but is pure otherwise.
 *
 * A method calling a `ExternallyPure` method can be `Pure` if the receiver of the call is confined
 * inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ExternallyPure extends Purity {
    final val flags = ExternallyPureFlags
}

/**
 * The respective method may modify its receiver, but otherwise it is side-effect free, i.e. it does
 * not have side effects but its results may still be non-deterministic.
 *
 * A method calling a `ExternallySideEffectFree` method can be `SideEffectFree` if the receiver of
 * the call is confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object ExternallySideEffectFree extends Purity {
    final val flags = ExternallySideEffectFreeFlags
}*/

/**
 * The respective method may modify its parameters, but is pure otherwise.
 *
 * A method calling a `ContextuallyPure` method can be `Pure` if the parameters of the call are
 * confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case class ContextuallyPure(override val modifiedParams: IntTrieSet) extends Purity {
    final val flags = ContextuallyPureFlags

    override def meet(other: Purity): Purity = other match {
        case _: ClassifiedImpure            => other
        case ContextuallyPure(p)            => ContextuallyPure(this.modifiedParams ++ p)
        case ContextuallySideEffectFree(p)  => ContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallyPure(p)           => DContextuallyPure(this.modifiedParams ++ p)
        case DContextuallySideEffectFree(p) => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case _ =>
            (other.flags: @switch) match {
                case CompileTimePure.flags | Pure.flags => this
                // For non-pure levels, we don't have compile-time purity anymore
                case _ => (other.flags | NotCompileTimePure: @switch) match {
                    case SideEffectFree.flags  => ContextuallySideEffectFree(modifiedParams)
                    case DPure.flags           => DContextuallyPure(modifiedParams)
                    case DSideEffectFree.flags => DContextuallySideEffectFree(modifiedParams)
                }
            }
    }
}

/**
 * The respective method may modify its parameters, but otherwise it is side-effect free, i.e. it
 * does not have side effects but its results may still be non-deterministic.
 *
 * A method calling a `ConteuxtuallySideEffectFree` method can be `SideEffectFree` if the parameters
 * of the call are confined inside that method.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case class ContextuallySideEffectFree(override val modifiedParams: IntTrieSet) extends Purity {
    final val flags = ContextuallySideEffectFreeFlags

    override def meet(other: Purity): Purity = other match {
        case _: ClassifiedImpure            => other
        case ContextuallyPure(p)            => ContextuallySideEffectFree(this.modifiedParams ++ p)
        case ContextuallySideEffectFree(p)  => ContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallyPure(p)           => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallySideEffectFree(p) => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case _ =>
            (other.flags: @switch) match {
                case CompileTimePure.flags | Pure.flags => this
                // For non-pure levels, we don't have compile-time purity anymore
                case _ => (other.flags | NotCompileTimePure: @switch) match {
                    case SideEffectFree.flags => this
                    case DPure.flags | DSideEffectFree.flags =>
                        DContextuallySideEffectFree(modifiedParams)
                }
            }
    }
}

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure. Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object DPure extends Purity {
    final val flags = PureFlags | PerformsDomainSpecificOperations
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object DSideEffectFree extends Purity {
    final val flags = SideEffectFreeFlags | PerformsDomainSpecificOperations
}
/*
/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure and it may modify its receiver.
 * Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object DExternallyPure extends Purity {
    final val flags = ExternallyPureFlags | PerformsDomainSpecificOperations
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure and it may modify its receiver. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case object DExternallySideEffectFree extends Purity {
    final val flags = ExternallySideEffectFreeFlags | PerformsDomainSpecificOperations
}*/

/**
 * The respective method may perform actions that are generally considered impure or
 * non-deterministic that some clients may wish to treat as pure and it may modify its parameters.
 * Otherwise it is pure.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case class DContextuallyPure(override val modifiedParams: IntTrieSet) extends Purity {
    final val flags = ContextuallyPureFlags | PerformsDomainSpecificOperations

    override def meet(other: Purity): Purity = other match {
        case _: ClassifiedImpure            => other
        case ContextuallyPure(p)            => DContextuallyPure(this.modifiedParams ++ p)
        case ContextuallySideEffectFree(p)  => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallyPure(p)           => DContextuallyPure(this.modifiedParams ++ p)
        case DContextuallySideEffectFree(p) => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case _ =>
            (other.flags: @switch) match {
                case CompileTimePure.flags | Pure.flags => this
                // For non-pure levels, we don't have compile-time purity anymore
                case _ => (other.flags | NotCompileTimePure: @switch) match {
                    case SideEffectFree.flags | DSideEffectFree.flags =>
                        DContextuallySideEffectFree(modifiedParams)
                    case DPure.flags => this
                }
            }
    }
}

/**
 * The respective method may perform actions that are generally considered impure that some clients
 * may wish to treat as pure and it may modify its parameters. Otherwise it is side-effect free.
 *
 * @see [[Purity]] for further details regarding the purity levels.
 */
case class DContextuallySideEffectFree(override val modifiedParams: IntTrieSet) extends Purity {
    final val flags = ContextuallySideEffectFreeFlags | PerformsDomainSpecificOperations

    override def meet(other: Purity): Purity = other match {
        case _: ClassifiedImpure            => other
        case ContextuallyPure(p)            => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case ContextuallySideEffectFree(p)  => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallyPure(p)           => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case DContextuallySideEffectFree(p) => DContextuallySideEffectFree(this.modifiedParams ++ p)
        case _                              => this
    }
}

/**
 * Clients have to treat the method as impure. If the property is refinable clients can keep
 * the dependency.
 */
sealed abstract class ClassifiedImpure extends Purity {
    final val flags = ImpureFlags
    override val withoutContextual: ClassifiedImpure = this
}

/**
 * The method needs to be treated as impure for the time being. However, the current
 * analysis is not able to derive a more precise result; no more dependency exist.
 */
case object ImpureByAnalysis extends ClassifiedImpure {
    override def meet(other: Purity): Purity = {
        other match {
            case ImpureByLackOfInformation => ImpureByLackOfInformation
            case _                         => this
        }
    }
}

/** The method is (finally classified as) impure; this also models the fallback. */
case object ImpureByLackOfInformation extends ClassifiedImpure {
    override def meet(other: Purity): Purity = this
}
