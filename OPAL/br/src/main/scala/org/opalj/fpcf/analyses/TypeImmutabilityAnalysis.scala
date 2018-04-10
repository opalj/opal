/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.ImmutableContainerType
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.MutableType
import org.opalj.fpcf.properties.TypeImmutability

/**
 * Determines the mutability of a specific type by checking if all subtypes of a specific
 * type are immutable and checking that the set of types is closed.
 *
 * @author Michael Eichberg
 */
class TypeImmutabilityAnalysis( final val project: SomeProject) extends FPCFAnalysis {

    def doDetermineTypeMutability(typeExtensibility: ObjectType ⇒ Answer)(e: Entity): PropertyComputationResult = e match {
        case t: ObjectType ⇒ step1(typeExtensibility)(t)
        case _ ⇒
            val m = e.getClass.getSimpleName+" is not an org.opalj.br.ObjectType"
            throw new IllegalArgumentException(m)
    }

    /**
     * @param t An object type which is not `java.lang.Object`.
     */
    def step1(
        typeExtensibility: ObjectType ⇒ Answer
    )(
        t: ObjectType
    ): PropertyComputationResult = {
        typeExtensibility(t) match {
            case Yes | Unknown ⇒ Result(t, MutableType)
            case No            ⇒ step2(t)
        }
    }

    def step2(t: ObjectType): PropertyComputationResult = {
        val directSubtypes = classHierarchy.directSubtypesOf(t)

        val cf = project.classFile(t)
        if (cf.exists(_.isFinal) || directSubtypes.isEmpty /*... the type is not extensible*/ ) {

            val c = new OnUpdateContinuation { c ⇒
                def apply(eps: SomeEPS): PropertyComputationResult = {
                    eps.ub match {
                        case p: ClassImmutability ⇒
                            val thisP = p.correspondingTypeImmutability
                            if (eps.isFinal)
                                Result(t, thisP)
                            else
                                IntermediateResult(t, MutableType, thisP, Seq(eps), c)
                    }
                }
            }

            ps(t, ClassImmutability.key) match {
                case FinalEP(_, p) ⇒
                    Result(t, p.correspondingTypeImmutability)
                case eps @ IntermediateEP(_, _, p) ⇒
                    val thisP = p.correspondingTypeImmutability
                    IntermediateResult(t, MutableType, thisP, Seq(eps), c)
                case epk ⇒
                    val dependees = Traversable(epk)
                    IntermediateResult(t, MutableType, ImmutableType, dependees, c)
            }
        } else {
            var dependencies = Map.empty[Entity, EOptionP[Entity, TypeImmutability]]
            var joinedImmutability: TypeImmutability = ImmutableType // this may become "Mutable..."
            var maxImmutability: TypeImmutability = ImmutableType

            directSubtypes foreach { subtype ⇒
                ps(subtype, TypeImmutability.key) match {
                    case FinalEP(_, ImmutableType) ⇒

                    case EPS(_, _, MutableType) ⇒
                        return Result(t, MutableType);

                    case FinalEP(_, subtypeP @ ImmutableContainerType) ⇒
                        joinedImmutability = joinedImmutability.meet(subtypeP)
                        maxImmutability = subtypeP

                    case eps @ IntermediateEP(_, subtypeP, _) ⇒
                        joinedImmutability = joinedImmutability.meet(subtypeP)
                        dependencies += ((subtype, eps))

                    case epk ⇒
                        joinedImmutability = MutableType
                        dependencies += ((subtype, epk))

                }
            }

            if (dependencies.isEmpty) {
                Result(t, maxImmutability)
            } else if (joinedImmutability == maxImmutability) {
                // E.g., as soon as one subtype is ConditionallyImmutable, we are at most
                // ConditionallyImmutable, even if all other subtype may even be immutable!
                Result(t, joinedImmutability)
            } else {
                // when we reach this point, we have dependencies to types for which
                // we have non-final information; joinedImmutability is either MutableType
                // or ConditionallyMutableType
                def c(eps: EPS[Entity, Property]): PropertyComputationResult = {

                    ///*debug*/ val previousDependencies = dependencies
                    ///*debug*/ val previousJoinedImmutability = joinedImmutability

                    def nextResult(): PropertyComputationResult = {
                        if (dependencies.isEmpty) {
                            Result(t, maxImmutability)
                        } else {
                            joinedImmutability = maxImmutability
                            val depIt = dependencies.values.iterator
                            var continue = true
                            while (continue && depIt.hasNext) {
                                val n = depIt.next()
                                if (n.hasProperty)
                                    joinedImmutability = joinedImmutability.meet(n.lb)
                                else {
                                    joinedImmutability = MutableType
                                    continue = false
                                }
                            }
                            if (joinedImmutability == maxImmutability) {
                                assert(maxImmutability == ImmutableContainerType)
                                Result(t, maxImmutability)
                            } else {
                                IntermediateResult(t, joinedImmutability, maxImmutability, dependencies.values, c)
                            }
                        }
                    }

                    eps match {
                        case FinalEP(e, ImmutableType) ⇒
                            dependencies = dependencies - e
                            nextResult()

                        case EPS(_, _, MutableType) ⇒
                            Result(t, MutableType)

                        case FinalEP(e, subtypeP @ ImmutableContainerType) ⇒
                            maxImmutability = ImmutableContainerType
                            dependencies = dependencies - e
                            nextResult()

                        case eps @ IntermediateEP(e, _, subtypeP: TypeImmutability) ⇒
                            dependencies = dependencies.updated(
                                e, eps.asInstanceOf[EOptionP[Entity, TypeImmutability]]
                            )
                            maxImmutability = maxImmutability.meet(subtypeP)
                            nextResult()
                    }
                }

                IntermediateResult(t, joinedImmutability, maxImmutability, dependencies.values, c)
            }
        }
    }
}

trait TypeImmutabilityAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(TypeImmutability)

    override def uses: Set[PropertyKind] = Set(ClassImmutability)
}

/**
 * Starter for the '''type immutability analysis'''.
 *
 * @author Michael Eichberg
 */
object EagerTypeImmutabilityAnalysis
        extends TypeImmutabilityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val typeExtensibility = project.get(TypeExtensibilityKey)
        val analysis = new TypeImmutabilityAnalysis(project)

        // An optimization, if the analysis also includes the JDK.
        ps.set(ObjectType.Object, MutableType)

        val types = project.allClassFiles.filter(_.thisType ne ObjectType.Object).map(_.thisType)

        ps.scheduleForEntities(types) {
            analysis.step1(typeExtensibility)
        }

        analysis
    }

}

object LazyTypeImmutabilityAnalysis
        extends TypeImmutabilityAnalysisScheduler with FPCFLazyAnalysisScheduler {
    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override protected[fpcf] def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {

        val typeExtensibility = p.get(TypeExtensibilityKey)
        val analysis = new TypeImmutabilityAnalysis(p)

        // An optimization, if the analysis also includes the JDK.
        ps.set(ObjectType.Object, MutableType)

        ps.registerLazyPropertyComputation(TypeImmutability.key, analysis.step1(typeExtensibility))
        analysis

    }
}
