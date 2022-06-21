/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

import org.opalj.da.ClassFile
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectIndex
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.VirtualMethodInvocationInstruction

/**
 * Groups test case features that test the support for libraries/partial programs. All test cases
 * assume that all packages are closed!!!!
 *
 * @note The features represent the __Library__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class Library(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq( // CBS = call-by-signature
            "LIB1", /* 0 --- parameter of library entry points must be resolved to any subtype */
            "LIB2", /* 1 --- calls on publically writeable fields must resolved to any subtype */
            "LIB3", /* 2 --- cbs with public classes only.  */
            "LIB4", /* 3 --- cbs with an internal class */
            "LIB5" /* 4 --- cbs with an internal class that has subclasses */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val projectIndex = project.get(ProjectIndexKey)
        var cbsCache = Map.empty[String, Set[Method]]
        //val declaredMethods = project.get(DeclaredMethodsKey)

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            fieldTypes = classFile.fields.filter(f => f.isNotFinal && !f.isPrivate).collect {
                case f: Field if f.fieldType.id >= 0 => f.fieldType.id
            }
            method @ MethodWithBody(body) <- classFile.methods
            paramTypes = method.parameterTypes.map(_.id).filter(_ >= 0)
            if (fieldTypes.nonEmpty || paramTypes.nonEmpty)
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case iv: INVOKEVIRTUAL   => iv
                case ii: INVOKEINTERFACE => ii
            }: PartialFunction[Instruction, VirtualMethodInvocationInstruction])
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val receiverType = invokeKind.declaringClass
            val otID = receiverType.id
            val isFieldType = fieldTypes.contains(otID)

            if (isFieldType) {
                instructionLocations(1) += l
            }

            val isParameterType = paramTypes.contains(otID)
            if (isParameterType) {
                if (invokeKind.opcode == INVOKEINTERFACE.opcode) {
                    val INVOKEINTERFACE(declClass, name, descriptor) = invokeKind
                    val cfO = project.classFile(declClass)
                    if (cfO.nonEmpty) {
                        val cf = cfO.get
                        val cacheKey = s"${cf.thisType.id}-$name-${descriptor.toJVMDescriptor}"
                        val targets = cbsCache.get(cacheKey) match {
                            case None => {
                                val newTargets = cf.findMethod(name, descriptor)
                                    .map(getCBSTargets(projectIndex, project, _))
                                    .getOrElse(Set.empty[Method])
                                cbsCache = cbsCache + ((cacheKey, newTargets))
                                newTargets
                            }
                            case Some(result) => result
                        }

                        var publicTarget = false
                        var packagePrivateTarget = false
                        var subclassTarget = false

                        val itr = targets.iterator
                        while (itr.hasNext
                            && !(publicTarget && packagePrivateTarget && subclassTarget)) {

                            val target = itr.next()
                            val declClass = target.classFile

                            publicTarget |= declClass.isPublic
                            packagePrivateTarget |= declClass.isPackageVisible
                            // this is a compiler dependent approximation
                            subclassTarget |= target.isSynthetic && target.isBridge
                        }

                        if (publicTarget)
                            instructionLocations(2) += l

                        if (packagePrivateTarget)
                            instructionLocations(3) += l

                        if (subclassTarget)
                            instructionLocations(4) += l
                    }
                }

                instructionLocations(0) += l
            }

        }

        ArraySeq.unsafeWrapArray(instructionLocations)

    }

    def getCBSTargets(
        projectIndex: ProjectIndex,
        project:      SomeProject,
        method:       Method
    ): Set[Method] = {

        val methodName = method.name
        val methodDescriptor = method.descriptor

        import projectIndex.findMethods

        val interfaceClassFile = method.classFile
        val interfaceType = interfaceClassFile.thisType

        val cbsTargets = mutable.Set.empty[Method]

        def analyzePotentialCBSTarget(cbsCallee: Method): Unit = {
            if (!cbsCallee.isPublic)
                return ;

            if (cbsCallee.isAbstract)
                return ;

            if (!isInheritableMethod(cbsCallee))
                return ;

            val cbsCalleeDeclaringClass = cbsCallee.classFile

            if (!cbsCalleeDeclaringClass.isClassDeclaration)
                return ;

            if (cbsCalleeDeclaringClass.isEffectivelyFinal)
                return ;

            val cbsCalleeDeclaringType = cbsCalleeDeclaringClass.thisType

            if (cbsCalleeDeclaringType eq ObjectType.Object)
                return ;

            if (project.classHierarchy.isSubtypeOf(
                cbsCalleeDeclaringType, interfaceType
            ))
                return ;

            if (hasSubclassWhichInheritsFromInterface(cbsCalleeDeclaringType, interfaceType, methodName, methodDescriptor, project).isYes)
                return ;

            cbsTargets += cbsCallee
        }

        findMethods(methodName, methodDescriptor) foreach analyzePotentialCBSTarget

        cbsTargets.toSet
    }

    @inline private[this] def isInheritableMethod(method: Method): Boolean = {

        !method.isPrivate
    }

    private[this] def hasSubclassWhichInheritsFromInterface(
        classType:        ObjectType,
        interfaceType:    ObjectType,
        methodName:       String,
        methodDescriptor: MethodDescriptor,
        project:          SomeProject
    ): Answer = {

        val classHierarchy = project.classHierarchy

        val itr = classHierarchy.allSubclassTypes(classType, reflexive = false)
        var isUnknown = false

        while (itr.hasNext) {
            val subtype = itr.next()
            project.classFile(subtype) match {
                case Some(subclassFile) =>
                    if (subclassFile.findMethod(methodName, methodDescriptor).isEmpty
                        && classHierarchy.isSubtypeOf(subtype, interfaceType))
                        return Yes;
                case None =>
                    isUnknown = false
            }
        }

        if (isUnknown)
            Unknown
        else
            No
    }
}