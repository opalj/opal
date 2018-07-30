/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.MethodWithBody
import org.opalj.br.Field
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.da.ClassFile

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
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            fieldTypes = classFile.fields.filter(f ⇒ f.isNotFinal && !f.isPrivate).collect {
                case f: Field if f.fieldType.id >= 0 ⇒ f.fieldType.id
            }
            method @ MethodWithBody(body) ← classFile.methods
            paramTypes = method.parameterTypes.map(_.id).filter(_ >= 0)
            if (fieldTypes.nonEmpty || paramTypes.nonEmpty)
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation ← body collect {
                case iv: INVOKEVIRTUAL   ⇒ iv
                case ii: INVOKEINTERFACE ⇒ ii
            }
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

            //            val ai = new InterruptableAI[Domain]
            //
            //            if (true) {
            //                val domain = new DefaultDomainWithCFGAndDefUse(project, method)
            //                val result = ai(method, domain)
            //
            //                //                invokeKind match {
            //                //                    case INVOKEVIRTUAL(declClass, _, _) if declClass.isObjectType ⇒ {
            //                //                        val ot = declClass.asObjectType
            //                //                        if (fieldTypes.contains(ot))
            //                //
            //                //
            //                //                    }
            //                //                    case INVOKEINTERFACE.opcode ⇒ {
            //                //                        ???
            //                //                    }
            //                //                }
            //            }
        }

        instructionLocations;

    }
}