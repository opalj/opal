/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.da.ClassFile

/**
 * Groups test case features that perform a direct method call.
 *
 * @note The features represent the __DirectCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class DirectCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "DC1", /* 0 --- static method call */
            "DC2", /* 1 --- constructor call */
            "DC3", /* 2 --- call on super */
            "DC4", /* 3 --- private method call */
            "DC5", /* 4 --- static call to an interface (private or not) */
            "DC6" /* 5 --- private method call with interface receiver */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(6)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case spec: INVOKESPECIAL ⇒ spec
                case stat: INVOKESTATIC  ⇒ stat
            }
        } {
            val pc = pcAndInvocation.pc
            val invokeKind = pcAndInvocation.value
            val declType = classFile.thisType

            val l = InstructionLocation(methodLocation, pc)

            val kindID = invokeKind match {
                case _@ INVOKESTATIC(declaringClass, _, _, _) ⇒ {
                    val cf = project.classFile(declaringClass)
                    if (cf.isEmpty)
                        -1
                    else if (cf.get.isInterfaceDeclaration)
                        4 /* static interface receiver */
                    else
                        0 /* static call to class type*/
                }
                case invSpec @ INVOKESPECIAL(declaringClass, _, name, _) ⇒ {
                    if (name != "<init>") {
                        if (declType eq declaringClass) {
                            if (project.specialCall(classFile.thisType, invSpec).value.isPrivate) {
                                val cf = project.classFile(declaringClass)
                                if (cf.isEmpty)
                                    -1
                                else if (cf.get.isInterfaceDeclaration)
                                    5
                                else
                                    2
                            } else {
                                -1
                            }
                        } else {
                            // call on super
                            3
                        }
                    } else {
                        val superTypes = project.classHierarchy.directSupertypes(declType)
                        val isSuperConstructor = superTypes.contains(declaringClass)
                        if ((declaringClass ne ObjectType.Object) && !isSuperConstructor) {
                            1
                        } else -1
                    }
                }
            }

            if (kindID >= 0) {
                instructionsLocations(kindID) += l
            }
        }

        instructionsLocations;
    }
}
