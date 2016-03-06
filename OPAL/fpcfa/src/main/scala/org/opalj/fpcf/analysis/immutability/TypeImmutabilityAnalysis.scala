/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package analysis
package immutability

import org.opalj.br.analyses.SomeProject
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.AnalysisException

class TypeImmutabilityAnalysis(val project: SomeProject) extends FPCFAnalysis {

    final def classHierarchy = project.classHierarchy

    /**
     * @param cf A class file which is not the class file of `java.lang.Object`.
     */
    def determineTypeImmutabilityApp(cf: ClassFile): PropertyComputationResult = {

        val directSubtypes = classHierarchy.directSubtypesOf(cf.thisType)
        if (cf.isFinal || /*APP:*/ directSubtypes.isEmpty) {
            ps(cf, ObjectImmutability.key) match {
                case Some(p: ObjectImmutability) ⇒
                    ImmediateResult(cf, p.correspondingTypeImmutability)
                case None ⇒
                    IntermediateResult(
                        cf, UnknownTypeImmutability,
                        Traversable(EPK(cf, ObjectImmutability.key)),
                        (e: Entity, p: Property, ut: UserUpdateType) ⇒ {
                            val objectImmutability = p.asInstanceOf[ObjectImmutability]
                            Result(cf, objectImmutability.correspondingTypeImmutability)
                        }
                    )
            }
        } else {
            val directSubclasses = directSubtypes.map(project.classFile(_).get)
            var dependencies = List.empty[EOptionP[ClassFile, TypeImmutability]]
            var joinedImmutability: TypeImmutability = ImmutableType // this may become "Unknown..."
            var maxImmutability: TypeImmutability = ImmutableType

            directSubclasses foreach { subclassFile ⇒
                ps(subclassFile, TypeImmutability.key) match {
                    case Some(ImmutableType) ⇒ /*ignore*/
                    case Some(MutableType) ⇒
                        return Result(cf, MutableType);
                    case Some(next @ ConditionallyImmutableType) ⇒
                        joinedImmutability = joinedImmutability.join(next)
                        maxImmutability = next
                    case Some(next @ AtLeastConditionallyImmutableType) ⇒
                        dependencies = EP(subclassFile, AtLeastConditionallyImmutableType) :: dependencies
                        joinedImmutability = joinedImmutability.join(next)
                    case Some(UnknownTypeImmutability) ⇒
                        dependencies = EP(subclassFile, UnknownTypeImmutability) :: dependencies
                        joinedImmutability = UnknownTypeImmutability
                    case None ⇒
                        dependencies = EPK(subclassFile, TypeImmutability.key) :: dependencies
                        joinedImmutability = UnknownTypeImmutability
                }
            }

            if (dependencies.isEmpty) {
                assert(
                    (maxImmutability eq ConditionallyImmutableType) ||
                        (maxImmutability eq ImmutableType)
                )
                Result(cf, maxImmutability)
            } else if (joinedImmutability eq ConditionallyImmutableType) {
                Result(cf, ConditionallyImmutableType)
            } else {
                // when we reach this point, we have dependencies to types for which
                // we have no further information (so far) or which are
                // AtLeastConditionallyImmutableType
                def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {

                    /*debug*/ val previousDependencies = dependencies
                    /*debug*/ val previousJoinedImmutability = joinedImmutability

                    def nextResult(): PropertyComputationResult = {
                        if (dependencies.isEmpty) {
                            Result(cf, maxImmutability)
                        } else {
                            joinedImmutability =
                                dependencies.foldLeft(
                                    maxImmutability
                                ) { (c, n) ⇒
                                    if (n.hasProperty)
                                        c.join(n.p)
                                    else
                                        UnknownTypeImmutability
                                }
                            if (joinedImmutability eq ConditionallyImmutableType)
                                Result(cf, ConditionallyImmutableType)
                            else {
                                assert(joinedImmutability.isRefineable)
                                assert(
                                    previousDependencies != dependencies ||
                                        previousJoinedImmutability != joinedImmutability,
                                    s"${cf.thisType.toJava}:::$e($p): dependencies and result were not updated: "+
                                        s"$previousDependencies => $dependencies; "+
                                        s"$previousJoinedImmutability => $joinedImmutability"
                                )
                                IntermediateResult(cf, joinedImmutability, dependencies, c)
                            }
                        }
                    }

                    p match {
                        case MutableType ⇒
                            Result(cf, MutableType)

                        case UnknownTypeImmutability ⇒
                            dependencies = dependencies map {
                                case EPK(`e`, _) ⇒ EP(e.asInstanceOf[ClassFile], UnknownTypeImmutability)
                                case d           ⇒ d
                            }
                            assert(joinedImmutability eq UnknownTypeImmutability)
                            assert(
                                previousDependencies != dependencies ||
                                    previousJoinedImmutability != joinedImmutability,
                                s"${cf.thisType.toJava}:::$e($p): dependencies and result were not updated: "+
                                    s"$previousDependencies => $dependencies; "+
                                    s"$previousJoinedImmutability => $joinedImmutability"
                            )
                            IntermediateResult(cf, joinedImmutability, dependencies, c)
                        case ImmutableType ⇒
                            dependencies = dependencies.filter(_.e ne e)
                            nextResult()
                        case ConditionallyImmutableType ⇒
                            dependencies = dependencies.filter(_.e ne e)
                            maxImmutability = ConditionallyImmutableType
                            nextResult()
                        case AtLeastConditionallyImmutableType ⇒
                            dependencies = dependencies map {
                                case EPK(`e`, _) | EP(`e`, _) ⇒ EP(e.asInstanceOf[ClassFile], AtLeastConditionallyImmutableType)
                                case d                        ⇒ d
                            }
                            nextResult()
                    }
                }
                IntermediateResult(cf, joinedImmutability, dependencies, c)
            }
        }
    }

}

object TypeImmutabilityAnalysis extends FPCFAnalysisRunner {

    override def recommendations: Set[FPCFAnalysisRunner] = Set.empty

    override def derivedProperties: Set[PropertyKind] = Set(TypeImmutability)

    override def usedProperties: Set[PropertyKind] = Set(ObjectImmutability)

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new TypeImmutabilityAnalysis(project)
        val classHierarchy = project.classHierarchy

        // all classes that do not have complete superclass information are mutable
        // due to lack of knowledge
        val mutableTypes = classHierarchy.rootClassTypes.
            filter { rt ⇒ rt ne ObjectType.Object }.
            map { rt ⇒ classHierarchy.allSubtypes(rt, reflexive = true) }.flatten.
            map { ot ⇒ project.classFile(ot) }.flatten.toSet

        assert(
            mutableTypes forall { cf ⇒ !cf.isInterfaceDeclaration },
            s"${mutableTypes.map(_.thisType.toJava).mkString(",")} contains interfaces"
        )

        mutableTypes foreach { cf ⇒ ps.handleResult(ImmediateResult(cf, MutableType)) }

        // the initial set of classes for which we want to determine the mutability

        /*just an
         *optimization: */ project.classFile(ObjectType.Object) foreach { ps.set(_, MutableType) }
        ps <||< (
            { case cf: ClassFile if !mutableTypes.contains(cf) && !(cf.thisType eq ObjectType.Object) ⇒ cf },
            analysis.determineTypeImmutabilityApp
        )

        analysis
    }

}
