/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq

import org.opalj.da.ClassFile
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.Instruction

/**
 * Groups test case features that perform a direct method call.
 *
 * @note The features represent the __DirectCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class NonVirtualCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "NVC1", /* 0 --- static method call */
            "NVC2", /* 1 --- constructor call */
            "NVC3", /* 2 --- private method call */
            "NVC4", /* 3 --- super call on a transitive super method */
            "NVC5", /* 4 --- super call on a direct superclass' target */
            "NVC6" /* 5 --- private method call within an interface */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case spec: INVOKESPECIAL => spec
                case stat: INVOKESTATIC  => stat
            }: PartialFunction[Instruction, Instruction])
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value
            val declType = classFile.thisType

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind match {
                case _@ INVOKESTATIC(declaringClass, _, _, _) => {
                    val cf = project.classFile(declaringClass)
                    if (cf.isEmpty)
                        -1
                    else if (cf.get.isInterfaceDeclaration)
                        4 /* static interface receiver */
                    else
                        0 /* static call to class type*/
                }
                case invSpec @ INVOKESPECIAL(declaringClass, _, name, _) => {
                    if (name != "<init>") {
                        val callTargetResult = project.specialCall(classFile.thisType, invSpec)
                        if (callTargetResult.isEmpty) {
                            -1
                        } else {
                            val callTarget = callTargetResult.value
                            if (declType eq declaringClass) {
                                if (callTarget.isPrivate) {
                                    val cf = project.classFile(declaringClass)
                                    if (cf.isEmpty)
                                        -1
                                    else if (cf.get.isInterfaceDeclaration)
                                        5 /* private interface method call */
                                    else
                                        2 /* private method call */
                                } else {
                                    -1
                                }
                            } else {
                                if (callTarget.classFile.thisType eq classFile.superclassType.get) {
                                    4 /* call to direct super class */
                                } else {
                                    3 /* call to transitive super class */
                                }
                            }
                        }
                    } else {
                        val superTypes = project.classHierarchy.directSupertypes(declType)
                        val isSuperConstructor = superTypes.contains(declaringClass)
                        if ((declaringClass ne ObjectType.Object) && !isSuperConstructor) {
                            1 /* constructor call */
                        } else -1
                    }
                }
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }
}
