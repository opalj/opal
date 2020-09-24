/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.DeepImmutableClass
import org.opalj.br.fpcf.properties.DeepImmutableType
import org.opalj.br.fpcf.properties.DependentImmutableClass
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.MutableClass
import org.opalj.br.fpcf.properties.MutableType
import org.opalj.br.fpcf.properties.ShallowImmutableClass
import org.opalj.br.fpcf.properties.ShallowImmutableType
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.fpcf.ELUBP
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP

/**
 * Determines the immutability of a specific type by checking if all subtypes of a specific
 * type are immutable and checking that the set of types is closed.
 *
 * @author Michael Eichberg
 * @author Tobias Roth
 */
class L1TypeImmutabilityAnalysis( final val project: SomeProject) extends FPCFAnalysis {

    def doDetermineTypeImmutability_new(
        typeExtensibility: ObjectType ⇒ Answer
    )(
        e: Entity
    ): ProperPropertyComputationResult = e match {
        case t: ObjectType ⇒ step1(typeExtensibility)(t)
        case _             ⇒ throw new IllegalArgumentException(s"$e is not an ObjectType")
    }

    /**
     * @param t An object type which is not `java.lang.Object`.
     */
    def step1(
        typeExtensibility: ObjectType ⇒ Answer
    )(
        t: ObjectType
    ): ProperPropertyComputationResult =
        {
            val te = typeExtensibility(t)
            te match {
                case Yes | Unknown ⇒
                    Result(t, MutableType)
                case No ⇒ step2(t)
            }
        }

    def step2(t: ObjectType): ProperPropertyComputationResult = {
        val directSubtypes = classHierarchy.directSubtypesOf(t)

        val cf = project.classFile(t)
        if (cf.exists(_.isFinal) || directSubtypes.isEmpty /*... the type is not extensible*/ ) {

            val c = new ProperOnUpdateContinuation { c ⇒
                def apply(eps: SomeEPS): ProperPropertyComputationResult = {
                    eps match {
                        case ELUBP(_, lb: ClassImmutability, ub: ClassImmutability) ⇒
                            val thisLB = lb.correspondingTypeImmutability
                            val thisUB = ub.correspondingTypeImmutability
                            if (eps.isFinal)
                                Result(t, thisUB)
                            else
                                InterimResult(t, thisLB, thisUB, Seq(eps), c)
                    }
                }
            }

            val resultToMatch = ps(t, ClassImmutability.key)
            resultToMatch match {
                case x @ FinalP(p) ⇒ {
                    Result(t, p.correspondingTypeImmutability);
                }

                case eps @ InterimLUBP(lb, ub) ⇒
                    val thisUB = ub.correspondingTypeImmutability
                    val thisLB = lb.correspondingTypeImmutability
                    InterimResult(t, thisLB, thisUB, Seq(eps), c)
                case epk ⇒ {
                    InterimResult(t, MutableType, DeepImmutableType, Seq(epk), c)
                }
            }
        } else {
            var dependencies = Map.empty[Entity, EOptionP[Entity, Property]]
            var joinedImmutability: TypeImmutability = DeepImmutableType //this may become "Mutable..."
            var maxImmutability: TypeImmutability = DeepImmutableType

            val resultToMatch2 = ps(t, ClassImmutability.key)
            resultToMatch2 match {
                case FinalP(DeepImmutableClass) ⇒
                case FinalP(MutableClass) ⇒
                    return Result(t, MutableType);
                case FinalP(ShallowImmutableClass) ⇒
                    joinedImmutability = ShallowImmutableType
                    maxImmutability = ShallowImmutableType
                case FinalP(DependentImmutableClass) ⇒
                    joinedImmutability = DependentImmutableType
                    if (maxImmutability != ShallowImmutableType)
                        maxImmutability = DependentImmutableType

                case eps @ InterimLUBP(lb, ub) ⇒
                    joinedImmutability = lb.correspondingTypeImmutability
                    maxImmutability = ub.correspondingTypeImmutability
                    dependencies += (t -> eps)

                case eOptP ⇒
                    joinedImmutability = MutableType
                    dependencies += (t -> eOptP)
            }

            directSubtypes foreach { subtype ⇒
                ps(subtype, TypeImmutability.key) match {
                    case FinalP(DeepImmutableType) ⇒
                    case UBP(MutableType) ⇒
                        return Result(t, MutableType);

                    case FinalP(ShallowImmutableType) ⇒
                        joinedImmutability = joinedImmutability.meet(ShallowImmutableType)
                        maxImmutability = ShallowImmutableType

                    case FinalP(DependentImmutableType) ⇒
                        joinedImmutability = joinedImmutability.meet(DependentImmutableType)
                        if (maxImmutability != ShallowImmutableType)
                            maxImmutability = DependentImmutableType

                    case eps @ InterimLUBP(subtypeLB, subtypeUB) ⇒
                        joinedImmutability = joinedImmutability.meet(subtypeLB)
                        maxImmutability = maxImmutability.meet(subtypeUB)
                        dependencies += ((subtype, eps))

                    case epk ⇒
                        joinedImmutability = MutableType
                        dependencies += ((subtype, epk))
                }
            }

            if (dependencies.isEmpty) {
                Result(t, maxImmutability)
            } else if (joinedImmutability == maxImmutability) {
                // E.g., as soon as one subtype is shallow immutable, we are at most
                // shallow immutable, even if all other subtype may even be deep immutable!
                Result(t, joinedImmutability)
            } else {
                // when we reach this point, we have dependencies to types for which
                // we have non-final information; joinedImmutability is either MutableType
                // or ImmutableContainer
                def c(eps: EPS[Entity, Property]): ProperPropertyComputationResult = {

                    ///*debug*/ val previousDependencies = dependencies
                    ///*debug*/ val previousJoinedImmutability = joinedImmutability

                    def nextResult(): ProperPropertyComputationResult = {
                        if (dependencies.isEmpty) {
                            Result(t, maxImmutability)
                        } else {
                            joinedImmutability = maxImmutability
                            val depIt = dependencies.valuesIterator
                            var continue = true
                            while (continue && depIt.hasNext) {
                                val n = depIt.next()
                                if (n.hasLBP)
                                    n.lb match {
                                        case lb: TypeImmutability ⇒
                                            joinedImmutability = joinedImmutability.meet(lb)
                                        case lb: ClassImmutability ⇒
                                            joinedImmutability =
                                                joinedImmutability.meet(lb.correspondingTypeImmutability)
                                    }
                                else {
                                    joinedImmutability = MutableType
                                    continue = false
                                }
                            }
                            if (joinedImmutability == maxImmutability) {
                                assert(maxImmutability == ShallowImmutableType)
                                Result(t, maxImmutability)
                            } else {
                                InterimResult(
                                    t,
                                    joinedImmutability,
                                    maxImmutability,
                                    dependencies.values,
                                    c
                                )
                            }
                        }
                    }

                    (eps: @unchecked) match {
                        case FinalEP(e, DeepImmutableType | DeepImmutableClass) ⇒
                            dependencies = dependencies - e
                            nextResult()

                        case UBP(x) if (x == MutableType || x == MutableClass) ⇒
                            Result(t, MutableType) //MutableType)

                        case FinalEP(e, x) if (x == ShallowImmutableType || x == ShallowImmutableClass) ⇒
                            maxImmutability = ShallowImmutableType
                            dependencies = dependencies - e
                            nextResult()
                        case FinalEP(e, DependentImmutableClass | DependentImmutableType) ⇒ {
                            if (maxImmutability != ShallowImmutableType)
                                maxImmutability = DependentImmutableType
                            dependencies = dependencies - e
                            nextResult()
                        }
                        case eps @ InterimEUBP(e, subtypeP) ⇒
                            dependencies = dependencies.updated(e, eps)
                            subtypeP match {
                                case subtypeP: TypeImmutability ⇒
                                    maxImmutability = maxImmutability.meet(subtypeP)
                                case subtypeP: ClassImmutability ⇒
                                    maxImmutability = maxImmutability.meet(subtypeP.correspondingTypeImmutability)
                            }
                            nextResult()
                    }
                }
                InterimResult(t, joinedImmutability, maxImmutability, dependencies.values, c)
            }
        }
    }
}

trait L1TypeImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(TypeImmutability)

    final override def uses: Set[PropertyBounds] =
        PropertyBounds.lubs(ClassImmutability, TypeImmutability)

}

/**
 * Starter for the '''type immutability analysis'''.
 *
 * @author Michael Eichberg
 */
object EagerL1TypeImmutabilityAnalysis extends L1TypeImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(project: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val typeExtensibility = project.get(TypeExtensibilityKey)
        val analysis = new L1TypeImmutabilityAnalysis(project)
        val types = project.allClassFiles.iterator.filter(_.thisType ne ObjectType.Object).map(_.thisType)
        ps.scheduleEagerComputationsForEntities(types) {
            analysis.step1(typeExtensibility)
        }
        analysis
    }
}

object LazyL1TypeImmutabilityAnalysis extends L1TypeImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val typeExtensibility = p.get(TypeExtensibilityKey)
        val analysis = new L1TypeImmutabilityAnalysis(p)
        val analysisRunner: ProperPropertyComputation[Entity] =
            analysis.doDetermineTypeImmutability_new(typeExtensibility)
        ps.registerLazyPropertyComputation(TypeImmutability.key, analysisRunner)
        analysis
    }
}
