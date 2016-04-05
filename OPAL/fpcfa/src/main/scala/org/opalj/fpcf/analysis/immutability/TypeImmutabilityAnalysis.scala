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
import org.opalj.log.OPALLogger
import org.opalj.fpcf.analysis.extensibility.IsExtensible

class TypeImmutabilityAnalysis( final val project: SomeProject) extends FPCFAnalysis {

    final val extensibleClasses = propertyStore.entities(IsExtensible)

    /**
     * @param cf A class file which is not the class file of `java.lang.Object`.
     */
    def determineTypeImmutability(cf: ClassFile): PropertyComputationResult = {

        if (extensibleClasses.contains(cf))
            return ImmediateResult(cf, MutableType);

        val directSubtypes = classHierarchy.directSubtypesOf(cf.thisType)

        if (cf.isFinal || /*APP:*/ directSubtypes.isEmpty) {

            def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {
                p match {
                    case UnknownObjectImmutability ⇒
                        val dependees = Traversable(EP(e, p))
                        IntermediateResult(cf, UnknownTypeImmutability, dependees, c)
                    case AtLeastConditionallyImmutableObject ⇒
                        val dependees = Traversable(EP(e, p))
                        IntermediateResult(cf, AtLeastConditionallyImmutableType, dependees, c)
                    case p: ObjectImmutability ⇒
                        assert(p.isFinal)
                        Result(cf, p.correspondingTypeImmutability)
                }
            }

            ps(cf, ObjectImmutability.key) match {
                case Some(p) ⇒
                    p match {
                        case p: MutableObject             ⇒ ImmediateResult(cf, MutableType)
                        case ImmutableObject              ⇒ ImmediateResult(cf, ImmutableType)
                        case ConditionallyImmutableObject ⇒ ImmediateResult(cf, ConditionallyImmutableType)
                        case AtLeastConditionallyImmutableObject | UnknownObjectImmutability ⇒
                            // In this case we have a class who's immutability (type) depends
                            // on the immutability of its object OR where the immutability is
                            // not yet determined
                            val typeImmutability: TypeImmutability = p.correspondingTypeImmutability
                            IntermediateResult(cf, typeImmutability, Traversable(EP(cf, p)), c)
                    }

                case None ⇒
                    val dependees = Traversable(EPK(cf, ObjectImmutability.key))
                    IntermediateResult(cf, UnknownTypeImmutability, dependees, c)
            }
        } else {
            val unavailableSubtype = directSubtypes.find(t ⇒ project.classFile(t).isEmpty)
            if (unavailableSubtype.isDefined) {
                val thisType = cf.thisType.toJava
                val subtype = unavailableSubtype.get.toJava
                OPALLogger.warn(
                    "project configuration",
                    s"the type $thisType's subtype $subtype is not available"
                )
                // Obviously the type hierarchy is incommplete;
                // hence, we have to make a safe and sound approximation!
                return Result(cf, MutableType)
            }

            val directSubclasses = directSubtypes map { subtype ⇒ project.classFile(subtype).get }

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
                /*
                assert(
                    (maxImmutability == ConditionallyImmutableType) ||
                        (maxImmutability == ImmutableType)
                )
                */
                Result(cf, maxImmutability)
            } else if (joinedImmutability == ConditionallyImmutableType) {
                Result(cf, ConditionallyImmutableType)
            } else {
                // when we reach this point, we have dependencies to types for which
                // we have no further information (so far) or which are
                // AtLeastConditionallyImmutableType
                def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {

                    ///*debug*/ val previousDependencies = dependencies
                    ///*debug*/ val previousJoinedImmutability = joinedImmutability

                    def nextResult(): PropertyComputationResult = {
                        if (dependencies.isEmpty) {
                            Result(cf, maxImmutability)
                        } else {
                            joinedImmutability =
                                dependencies.foldLeft(maxImmutability) { (c, n) ⇒
                                    if (n.hasProperty)
                                        c.join(n.p)
                                    else
                                        UnknownTypeImmutability
                                }
                            if (joinedImmutability == ConditionallyImmutableType) {
                                Result(cf, ConditionallyImmutableType)
                            } else {
                                /* DEBUGGING
                                assert(joinedImmutability.isRefineable)
                                assert(
                                    previousDependencies != dependencies ||
                                        previousJoinedImmutability != joinedImmutability,
                                    s"${cf.thisType.toJava}:::\n$e($p):\ndependencies and result were not updated:\n"+
                                        s"$previousDependencies => $dependencies;\n"+
                                        s"$previousJoinedImmutability => $joinedImmutability"
                                )
                                maxImmutability.isValidSuccessorOf(joinedImmutability).foreach { s ⇒
                                    throw new AssertionError(s"associating $e with new property $p failed: $s")
                                }
                                */
                                IntermediateResult(cf, joinedImmutability, dependencies, c)
                            }
                        }
                    }

                    p match {
                        case MutableType ⇒ Result(cf, MutableType)

                        case UnknownTypeImmutability ⇒
                            //[DEBUG] var updated = false
                            dependencies = dependencies map {
                                case EPK(dependeeE, _) if dependeeE eq e ⇒
                                    //[DEBUG] updated = true
                                    EP(e.asInstanceOf[ClassFile], UnknownTypeImmutability)
                                case d ⇒
                                    d
                            }
                            /* DEBUGGING
                            assert(
                                updated,
                                s"${cf.thisType.toJava}: didn't find the dependeeE $e in ${dependencies.mkString("(", ",", ")")}"
                            )
                            assert(
                                joinedImmutability == UnknownTypeImmutability,
                                s"the previous joined immutability was $joinedImmutability but it was expected to be UnknownTypeImmutability"
                            )
                            assert(
                                previousDependencies != dependencies ||
                                    previousJoinedImmutability != joinedImmutability,
                                s"${cf.thisType.toJava}:\n$e($p):\ndependencies and result were not updated:\n"+
                                    s"$previousDependencies => $dependencies;\n"+
                                    s"$previousJoinedImmutability => $joinedImmutability"
                            )
                            */
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
                                case EPK(dependeeE, _) if dependeeE eq e ⇒
                                    EP(e.asInstanceOf[ClassFile], AtLeastConditionallyImmutableType)
                                case EP(dependeeE, _) if dependeeE eq e ⇒
                                    EP(e.asInstanceOf[ClassFile], AtLeastConditionallyImmutableType)
                                case d ⇒
                                    d
                            }
                            nextResult()
                    }
                }

                //[DEBUG] assert(joinedImmutability.isRefineable)
                IntermediateResult(cf, joinedImmutability, dependencies, c)
            }
        }
    }

}

/**
 * Starter for the '''type immutability analysis'''.
 *
 * @author Michael Eichberg
 */
object TypeImmutabilityAnalysis extends FPCFAnalysisRunner {

    override def recommendations: Set[FPCFAnalysisRunner] = Set.empty

    override def derivedProperties: Set[PropertyKind] = Set(TypeImmutability)

    override def usedProperties: Set[PropertyKind] = Set(ObjectImmutability, IsExtensible)

    def start(project: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new TypeImmutabilityAnalysis(project)

        // An optimization if the analysis also includes the JDK.
        project.classFile(ObjectType.Object) foreach { ps.set(_, MutableType) }

        ps <||< (
            { case cf: ClassFile if (cf.thisType ne ObjectType.Object) ⇒ cf },
            analysis.determineTypeImmutability
        )

        analysis
    }

}
