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

import org.opalj.log.OPALLogger
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.ConditionallyImmutableType
import org.opalj.fpcf.properties.MutableType
import org.opalj.fpcf.properties.ClassImmutability

/**
 * Determines the mutability of a specific type by checking if all subtypes of a specific
 * type are immutable and checking that the set of types is closed.
 *
 * @author Michael Eichberg
 */
class TypeImmutabilityAnalysis( final val project: SomeProject) extends FPCFAnalysis {

    /**
     * @param cf A class file which is not the class file of `java.lang.Object`.
     */
    def step1(
        typeExtensibility: ObjectType ⇒ Answer
    )(
        cf: ClassFile
    ): PropertyComputationResult = {
        typeExtensibility(cf.thisType) match {
            case Yes | Unknown ⇒ Result(cf.thisType, MutableType)
            case No            ⇒ step2(cf)
        }
    }

    def step2(cf: ClassFile): PropertyComputationResult = {
        val thisE = cf.thisType
        val directSubtypes = classHierarchy.directSubtypesOf(cf.thisType)

        if (cf.isFinal || directSubtypes.isEmpty /*... the type is not extensible*/ ) {

            val c = new OnUpdateContinuation { c ⇒
                def apply(e: Entity, p: Property, isFinal: Boolean): PropertyComputationResult = {
                    p match {
                        case p: ClassImmutability ⇒
                            val thisP = p.correspondingTypeImmutability
                            if (isFinal)
                                Result(thisE, thisP)
                            else
                                IntermediateResult(thisE, thisP, Seq(EPS(e, p, isFinal)), c)
                    }
                }
            }

            ps(thisE, ClassImmutability.key) match {
                case eps @ EPS(_, p, isFinal) ⇒
                    val thisP = p.correspondingTypeImmutability
                    if (isFinal)
                        Result(thisE, thisP)
                    else {
                        IntermediateResult(thisE, thisP, Seq(eps), c)
                    }

                case epk ⇒
                    val dependees = Traversable(epk)
                    IntermediateResult(thisE, MutableType, dependees, c)
            }
        } else {
            val unavailableSubtype = directSubtypes.find(t ⇒ project.classFile(t).isEmpty)
            if (unavailableSubtype.isDefined) {
                val thisType = thisE.toJava
                val subtype = unavailableSubtype.get.toJava
                OPALLogger.warn(
                    "project configuration", s"$thisType's subtype $subtype is not available"
                )
                // Obviously the type hierarchy is incomplete;
                // hence, we have to make a safe and sound approximation!
                return Result(thisE, MutableType);
            }

            val directSubclasses = directSubtypes map { subtype ⇒ project.classFile(subtype).get }

            var dependencies = Map.empty[Entity, EOptionP[Entity, TypeImmutability]]
            var joinedImmutability: TypeImmutability = ImmutableType // this may become "Mutable..."
            var maxImmutability: TypeImmutability = ImmutableType

            directSubclasses foreach { subclassFile ⇒
                val subtypeE = subclassFile.thisType
                ps(subtypeE, TypeImmutability.key) match {
                    case EPS(_, ImmutableType, isFinal) ⇒
                        assert(isFinal) /* otherwise ignore*/

                    case eps @ EPS(_, MutableType, isFinal) ⇒
                        if (isFinal) {
                            return Result(cf, MutableType);
                        }
                        joinedImmutability = MutableType
                        dependencies += ((subtypeE, eps))

                    case eps @ EPS(_, subtypeP @ ConditionallyImmutableType, isFinal) ⇒
                        joinedImmutability = joinedImmutability.meet(subtypeP)
                        if (isFinal) {
                            maxImmutability = subtypeP
                        } else {
                            dependencies += ((subtypeE, eps))
                        }

                    case epk ⇒
                        joinedImmutability = MutableType
                        dependencies += ((subtypeE, epk))

                }
            }

            if (dependencies.isEmpty) {
                Result(cf, maxImmutability)
            } else if (joinedImmutability == maxImmutability) {
                // E.g., as soon as one subtype is ConditionallyImmutable, we are at most
                // ConditionallyImmutable, even if all other subtype may even be immutable!
                Result(cf, joinedImmutability)
            } else {
                // when we reach this point, we have dependencies to types for which
                // we have non-final information; joinedImmutability is either MutableType
                // or ConditionallyMutableType
                def c(e: Entity, p: Property, isFinal: Boolean): PropertyComputationResult = {

                    ///*debug*/ val previousDependencies = dependencies
                    ///*debug*/ val previousJoinedImmutability = joinedImmutability

                    def nextResult(): PropertyComputationResult = {
                        if (dependencies.isEmpty) {
                            Result(thisE, maxImmutability)
                        } else {
                            joinedImmutability = maxImmutability
                            val depIt = dependencies.values.iterator
                            var continue = true
                            while (continue && depIt.hasNext) {
                                val n = depIt.next()
                                if (n.hasProperty)
                                    joinedImmutability = joinedImmutability.meet(n.p)
                                else {
                                    joinedImmutability = MutableType
                                    continue = false
                                }
                            }
                            if (joinedImmutability == maxImmutability) {
                                assert(maxImmutability == ConditionallyImmutableType)
                                Result(thisE, maxImmutability)
                            } else {
                                IntermediateResult(thisE, joinedImmutability, dependencies.values, c)
                            }
                        }
                    }

                    p match {
                        case ImmutableType ⇒
                            dependencies = dependencies - e
                            nextResult()

                        case p @ ConditionallyImmutableType ⇒
                            if (isFinal) {
                                maxImmutability = ConditionallyImmutableType
                                dependencies = dependencies - e
                            } else {
                                dependencies = dependencies.updated(e, EPS(e, p, isFinal))
                            }
                            nextResult()

                        case p @ MutableType ⇒
                            assert(joinedImmutability == MutableType)
                            if (isFinal) {
                                Result(thisE, MutableType)
                            } else {
                                dependencies = dependencies.updated(e, EPS(e, p, isFinal))
                                IntermediateResult(thisE, MutableType, dependencies.values, c)
                            }
                    }
                }

                IntermediateResult(thisE, joinedImmutability, dependencies.values, c)
            }
        }
    }
}

/**
 * Starter for the '''type immutability analysis'''.
 *
 * @author Michael Eichberg
 */
object TypeImmutabilityAnalysis extends FPCFEagerAnalysisScheduler {

    override def derives: Set[PropertyKind] = Set(TypeImmutability)

    override def uses: Set[PropertyKind] = Set(ClassImmutability)

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val typeExtensibility = project.get(TypeExtensibilityKey)
        val analysis = new TypeImmutabilityAnalysis(project)

        // An optimization, if the analysis also includes the JDK.
        project.classFile(ObjectType.Object) foreach { ps.set(_, MutableType) }

        ps.scheduleForEntities(project.allClassFiles.filter(_.thisType ne ObjectType.Object)) {
            analysis.step1(typeExtensibility)
        }

        analysis
    }

}
