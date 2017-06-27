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
package org.opalj.hermes.queries

import org.opalj.ai.{BaseAI, CorrelationalDomain, domain}
import org.opalj.br.analyses.{FieldAccessInformation, FieldAccessInformationKey, Project}
import org.opalj.br.instructions.{ArrayLoadInstruction, FieldReadAccess, FieldWriteAccess, LoadConstantInstruction, MethodInvocationInstruction, PUTFIELD, PUTSTATIC, ReturnValueInstruction, VirtualMethodInvocationInstruction}
import org.opalj.br.{ClassFile, Method}
import org.opalj.hermes._

/**
 * Counts which kinds of micro patterns are actually available.
 *
 * @author Leonid Glanz
 */
object MicroPatterns extends FeatureQuery {

    override val featureIDs: List[String] = {
        List(
            /*0*/ "Designator",
            /*1*/ "Taxonomy",
            /*2*/ "Joiner",
            /*3*/ "Pool",
            /*4*/ "Function Pointer",
            /*5*/ "Function Object",
            /*6*/ "Cobol Like",
            /*7*/ "Stateless",
            /*8*/ "Common State",
            /*9*/ "Immutable",
            /*10*/ "Restricted Creation",
            /*11*/ "Sampler",
            /*12*/ "Box",
            /*13*/ "Compound Box",
            /*14*/ "Canopy",
            /*15*/ "Record",
            /*16*/ "Data Manager",
            /*17*/ "Sink",
            /*18*/ "Outline",
            /*19*/ "Trait",
            /*20*/ "State Machine",
            /*21*/ "Pure Type",
            /*22*/ "Augmented Type",
            /*23*/ "Pseudo Class",
            /*24*/ "Implementor",
            /*25*/ "Overrider",
            /*26*/ "Extender"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(org.opalj.da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        val fa = project.get(FieldAccessInformationKey)

        val microPatternLocations = Array.fill(27)(new LocationsContainer[S])
        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
        } {
            val location = ClassFileLocation(source, classFile)

            if (isDesignator(classFile, project)) microPatternLocations(0) += location
            if (isTaxonomy(classFile)) microPatternLocations(1) += location
            if (isJoiner(classFile)) microPatternLocations(2) += location
            if (isPool(classFile)) microPatternLocations(3) += location
            if (isFunctionPointer(classFile)) microPatternLocations(4) += location
            if (isFunctionObject(classFile)) microPatternLocations(5) += location
            if (isCobolLike(classFile)) microPatternLocations(6) += location
            if (isStateless(classFile)) microPatternLocations(7) += location
            if (isCommonState(classFile)) microPatternLocations(8) += location
            if (isImmutable(classFile, fa)) microPatternLocations(9) += location
            if (isRestrictedCreation(classFile)) microPatternLocations(10) += location
            if (isSampler(classFile)) microPatternLocations(11) += location
            if (isBox(classFile, fa)) microPatternLocations(12) += location
            if (isCompoundBox(classFile, fa)) microPatternLocations(13) += location
            if (isCanopy(classFile, fa)) microPatternLocations(14) += location
            if (isRecord(classFile)) microPatternLocations(15) += location
            if (isDataManager(classFile, project)) microPatternLocations(16) += location
            if (isSink(classFile)) microPatternLocations(17) += location
            if (isOutline(classFile)) microPatternLocations(18) += location
            if (isTrait(classFile)) microPatternLocations(19) += location
            if (isStateMachine(classFile)) microPatternLocations(20) += location
            if (isPureType(classFile)) microPatternLocations(21) += location
            if (isAugmentedType(classFile)) microPatternLocations(22) += location
            if (isPseudoClass(classFile)) microPatternLocations(23) += location
            if (isImplementor(classFile, project)) microPatternLocations(24) += location
            if (isOverrider(classFile, project)) microPatternLocations(25) += location
            if (isExtender(classFile: ClassFile, project)) microPatternLocations(26) += location
        }
        for { (featureID, featureIDIndex) ← featureIDs.iterator.zipWithIndex } yield {
            Feature[S](featureID, microPatternLocations(featureIDIndex))
        }
    }

    def isDesignator[S](cl: ClassFile, theProject: Project[S]): Boolean = cl.interfaceTypes.isEmpty &&
        (cl.thisType.fqn.endsWith("/Object") ||
            (cl.methods.isEmpty &&
                cl.fields.isEmpty &&
                (!hasSuperType(cl) || theProject.classFile(cl.superclassType.get).isEmpty ||
                    isDesignator(theProject.classFile(cl.superclassType.get).
                        get, theProject)) &&
                    (cl.interfaceTypes.isEmpty || cl.interfaceTypes.forall(i ⇒ theProject.classFile(i).isEmpty ||
                        isDesignator(theProject.classFile(i).get, theProject)))))

    def isTaxonomy(cl: ClassFile): Boolean = cl.methods.isEmpty &&
        cl.fields.isEmpty && ((cl.interfaceTypes.size == 1 && !hasSuperType(cl)) ||
            (cl.interfaceTypes.isEmpty && hasSuperType(cl)))

    def isJoiner(cl: ClassFile): Boolean = (cl.methods.isEmpty || (!cl.isInterfaceDeclaration &&
        cl.methods.forall(m ⇒ isInitMethod(m)))) &&
        cl.fields.isEmpty && (cl.interfaceTypes.size > 1 || (hasSuperType(cl) && cl.interfaceTypes.nonEmpty))

    def isPool(cl: ClassFile): Boolean = cl.methods.forall { m ⇒
        (m.isConstructor && m.descriptor.parametersCount == 0) || isObjectMethod(m)
    } &&
        cl.fields.nonEmpty && cl.fields.forall { f ⇒ f.isFinal && f.isStatic }

    def isObjectMethod(method: Method): Boolean = method.name.equals("hashCode") ||
        method.name.equals("equals") || method.name.equals("getClass") ||
        method.name.equals("clone") || method.name.equals("toString") ||
        method.name.equals("notify") || method.name.equals("notifyAll") ||
        method.name.equals("wait") || method.name.equals("finalize")

    def isFunctionPointer(cl: ClassFile): Boolean =
        !cl.isInterfaceDeclaration && !cl.isAbstract && cl.methods.count { m ⇒
            !isInitMethod(m)
        } == 1 && cl.methods.count { m ⇒
            m.isPublic
        } == 1 && !cl.methods.exists(m ⇒ m.isStatic &&
            !m.isStaticInitializer) && cl.fields.isEmpty

    def isFunctionObject(cl: ClassFile): Boolean = cl.fields.nonEmpty &&
        cl.fields.forall { f ⇒ !f.isStatic } &&
        cl.methods.count { m ⇒
            !isInitMethod(m) && m.isPublic
        } == 1 &&
        !cl.methods.filter(m ⇒ !isInitMethod(m)).exists(m ⇒ m.isStatic)

    def isCobolLike(cl: ClassFile): Boolean = !cl.methods.exists { m ⇒
        !isInitMethod(m)
    } && cl.methods.count { m ⇒
        !isInitMethod(m) &&
            m.isStatic
    } == 1 &&
        !cl.fields.exists { f ⇒ !f.isStatic } && cl.fields.exists(f ⇒ f.isStatic)

    def isStateless(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration && !cl.isAbstract &&
        !cl.fields.exists { f ⇒ !(f.isFinal && f.isStatic) } &&
        cl.methods.count(m ⇒ !isInitMethod(m) && !isObjectMethod(m)) > 1

    def isCommonState(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.nonEmpty && cl.fields.forall { f ⇒ f.isStatic } &&
        cl.fields.exists { f ⇒ !f.isFinal }

    def isImmutable(cl: ClassFile, fa: FieldAccessInformation): Boolean = !cl.isInterfaceDeclaration &&
        !cl.isAbstract && cl.fields.count { f ⇒ !f.isStatic } > 1 &&
        cl.fields.forall(f ⇒ f.isPrivate && !f.isStatic) &&
        cl.fields.forall { f ⇒
            !fa.allWriteAccesses.contains(f) ||
                fa.allWriteAccesses(f).forall(p ⇒ isInitMethod(p._1))
        }

    def isRestrictedCreation(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.exists { f ⇒ f.isStatic && !f.isFinal && f.fieldType.toJava.equals(cl.thisType.toJava) } &&
        cl.methods.filter { m ⇒ m.isConstructor }.forall { m ⇒ m.isPrivate }

    def isSampler(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration &&
        cl.methods.filter { m ⇒ m.isConstructor }.exists { m ⇒ m.isPublic } &&
        cl.fields.exists { f ⇒
            f.isStatic &&
                f.fieldType.toJava.equals(cl.thisType.toJava)
        }

    def isBox(cl: ClassFile, fa: FieldAccessInformation): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.count { f ⇒ !f.isStatic } == 1 &&
        cl.fields.count { f ⇒ !f.isFinal } == 1 &&
        cl.fields.exists(f ⇒ fa.allWriteAccesses.contains(f) &&
            fa.allWriteAccesses(f).exists(t ⇒ cl.methods.contains(t._1)))

    def isCompoundBox(cl: ClassFile, fa: FieldAccessInformation): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.count(f ⇒ f.fieldType.isReferenceType && !f.isStatic &&
            !f.isFinal && fa.allWriteAccesses.contains(f) &&
            fa.allWriteAccesses(f).exists(t ⇒ cl.methods.contains(t._1))) == 1 &&
        cl.fields.count(f ⇒ !f.isStatic && !f.fieldType.isReferenceType) + 1 == cl.fields.size

    def isCanopy(cl: ClassFile, fa: FieldAccessInformation): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.count { f ⇒ !f.isStatic } == 1 &&
        cl.fields.count { f ⇒ !f.isStatic && !f.isPublic } == 1 &&
        cl.fields.exists { f ⇒
            !f.isStatic && fa.allWriteAccesses.contains(f) &&
                fa.allWriteAccesses(f).forall(p ⇒ isInitMethod(p._1))
        }

    def isRecord(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration && cl.fields.nonEmpty &&
        cl.fields.forall { f ⇒ f.isPublic } && cl.fields.exists(f ⇒ !f.isStatic) && cl.methods.forall(m ⇒ isInitMethod(m) || isObjectMethod(m))

    def isDataManager[S](cl: ClassFile, theProject: Project[S]): Boolean = !cl.isInterfaceDeclaration &&
        !cl.isAbstract &&
        cl.fields.nonEmpty &&
        cl.methods.count(m ⇒ !isInitMethod(m) && !isObjectMethod(m)) > 1 &&
        cl.methods.filter(m ⇒ !isInitMethod(m) && !isObjectMethod(m)).forall { m ⇒
            isSetter(m, cl, theProject) ||
                isGetter(m, cl, theProject)
        }

    def isGetter[S](method: Method, cf: ClassFile, theProject: Project[S]): Boolean = {
        if (!method.isPublic || method.returnType.isVoidType || method.body.isEmpty ||
            (method.body.isDefined && method.body.isEmpty) ||
            !method.body.get.instructions.exists { i ⇒ i.isInstanceOf[FieldReadAccess] }) {
            return false
        }

        val instructions = method.body.get.associateWithIndex().toMap
        val result = BaseAI(cf, method, new AnalysisDomain(theProject, method))
        val returns = instructions.filter(i ⇒ i._2.isInstanceOf[ReturnValueInstruction])

        returns.forall(r ⇒ result.domain.operandOrigin(r._1, 0).forall { u ⇒
            instructions.contains(u) && (instructions(u).isInstanceOf[FieldReadAccess] ||
                instructions(u).isInstanceOf[ArrayLoadInstruction] ||
                instructions(u).isInstanceOf[LoadConstantInstruction[_]])
        })
    }

    def isSetter[S](method: Method, cf: ClassFile, theProject: Project[S]): Boolean = {
        if (!method.isPublic || !method.returnType.isVoidType ||
            method.descriptor.parametersCount == 0 || method.body.isEmpty ||
            (method.body.isDefined && method.body.isEmpty) ||
            !method.body.get.instructions.exists { i ⇒ i.isInstanceOf[FieldWriteAccess] }) {
            return false
        }

        val instructions = method.body.get.associateWithIndex().toMap
        val result = BaseAI(cf, method, new AnalysisDomain(theProject, method))
        val puts = instructions.filter(i ⇒ i._2.isInstanceOf[FieldWriteAccess])

        puts.forall(p ⇒ (p._2.isInstanceOf[PUTSTATIC] &&
            result.domain.operandOrigin(p._1, 0).forall { x ⇒
                x < 0 || (instructions.contains(x) &&
                    instructions(x).isInstanceOf[LoadConstantInstruction[_]])
            }) ||
            (p._2.isInstanceOf[PUTFIELD] &&
                result.domain.operandOrigin(p._1, 1).forall { x ⇒
                    x < 0 || (instructions.contains(x) &&
                        instructions(x).isInstanceOf[LoadConstantInstruction[_]])
                }))
    }

    def isSink(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration &&
        cl.methods.exists { m ⇒ !isInitMethod(m) } &&
        cl.methods.forall { m ⇒
            m.body.isEmpty ||
                m.body.get.instructions.filter(i ⇒ i.isInstanceOf[MethodInvocationInstruction]).forall { i ⇒
                    !i.asInstanceOf[MethodInvocationInstruction].declaringClass.isObjectType ||
                        i.asInstanceOf[MethodInvocationInstruction].declaringClass.asObjectType.equals(cl.thisType)
                }
        }

    def isOutline(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration && cl.isAbstract && cl.methods.count { m ⇒
        m.body.isDefined && m.body.nonEmpty &&
            m.body.get.instructions.exists { i ⇒
                i.isInstanceOf[VirtualMethodInvocationInstruction] &&
                    i.asInstanceOf[VirtualMethodInvocationInstruction].declaringClass.equals(cl.thisType) &&
                    cl.methods.filter { x ⇒ x.isAbstract }.exists { x ⇒
                        x.name.equals(i.asInstanceOf[VirtualMethodInvocationInstruction].name) &&
                            x.descriptor.equals(i.asInstanceOf[VirtualMethodInvocationInstruction].methodDescriptor)
                    }
            }
    } > 1

    def isTrait(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration && cl.isAbstract &&
        cl.fields.isEmpty && cl.methods.count(m ⇒ m.isAbstract) > 0

    def isStateMachine(cl: ClassFile): Boolean = cl.methods.count(m ⇒ !isInitMethod(m)) > 1 &&
        cl.methods.forall { m ⇒ m.descriptor.parametersCount == 0 } && cl.fields.nonEmpty

    def isPureType(cl: ClassFile): Boolean = ((cl.isAbstract && cl.methods.nonEmpty &&
        cl.methods.forall { m ⇒ m.isAbstract && !m.isStatic }) || (cl.isInterfaceDeclaration && cl.methods.nonEmpty &&
            cl.methods.forall { m ⇒ !m.isStatic })) &&
            cl.fields.isEmpty

    def isAugmentedType(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration && cl.isAbstract &&
        cl.methods.forall { m ⇒ m.isAbstract } && cl.fields.size >= 3 &&
        cl.fields.forall { f ⇒ f.isFinal && f.isStatic } &&
        cl.fields.map { f ⇒ f.fieldType }.toSet.size == 1

    def isPseudoClass(cl: ClassFile): Boolean = !cl.isInterfaceDeclaration &&
        cl.fields.forall { f ⇒ f.isStatic } && cl.methods.nonEmpty &&
        cl.methods.forall { m ⇒ m.isAbstract || m.isStatic }

    def isImplementor[S](cl: ClassFile, theProject: Project[S]): Boolean = !cl.isInterfaceDeclaration &&
        !cl.isAbstract && cl.methods.exists { m ⇒
            m.isPublic &&
                !isInitMethod(m)
        } && cl.methods.forall { m ⇒
            isInitMethod(m) || !m.isPublic ||
                (theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor) match {
                    case Some(a) ⇒ (a.isAbstract || a.body.isEmpty) && (
                        (hasSuperType(cl) && theProject.classFile(a) != null &&
                            theProject.classFile(a).thisType == cl.superclassType.get) ||
                            cl.interfaceTypes.exists(it ⇒ theProject.classFile(a) != null && theProject.classFile(a).thisType == it)
                    )
                    case None ⇒ false
                })
        }

    def isInitMethod(method: Method): Boolean = method.isInitializer

    def hasSuperType(cl: ClassFile): Boolean = cl.superclassType.isDefined &&
        !cl.superclassType.get.fqn.endsWith("/Object")

    def isOverrider[S](cl: ClassFile, theProject: Project[S]): Boolean = !cl.isInterfaceDeclaration && !cl.isAbstract &&
        cl.methods.exists { m ⇒
            !isInitMethod(m)
        } && cl.methods.forall { m ⇒
            m.isInitializer ||
                (theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor) match {
                    case Some(a) ⇒ (!a.isAbstract && a.body.isDefined && m.body.nonEmpty) && (
                        (hasSuperType(cl) && theProject.classFile(a) != null &&
                            theProject.classFile(a).thisType == cl.superclassType.get) ||
                            cl.interfaceTypes.exists(it ⇒ theProject.classFile(a) != null && theProject.classFile(a).thisType == it)
                    )
                    case None ⇒ false
                })
        }

    def isExtender[S](cl: ClassFile, theProject: Project[S]): Boolean = !cl.isInterfaceDeclaration &&
        cl.methods.exists { m ⇒
            !isInitMethod(m)
        } && hasSuperType(cl) && cl.methods.forall { m ⇒
            isInitMethod(m) ||
                (theProject.resolveMethodReference(cl.thisType, m.name, m.descriptor) match {
                    case Some(_) ⇒ false
                    case None    ⇒ true
                })
        }

    class AnalysisDomain[S](val project: Project[S], val method: Method)

        extends CorrelationalDomain
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.DefaultLongValues
        with domain.l1.ConcretePrimitiveValuesConversions
        with domain.l1.LongValuesShiftOperators
        with domain.TheProject
        with domain.TheMethod
        with domain.RecordDefUse
}
