/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.da.ClassFile

import scala.collection.immutable.ArraySeq

/**
 * Test case feature that performs an interface call for a default method where an intermediate
 * interface shadows the default method with a static method (not valid java, but valid bytecode).
 *
 * @author Dominik Helm
 */
class NonJavaBytecode1(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq("NJ1")
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val instructionsLocations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val classHierarchy = project.classHierarchy

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInstruction <- body
            if pcAndInstruction.instruction.opcode == INVOKEINTERFACE.opcode
        } {
            val INVOKEINTERFACE(declaringClass, name, desc) = pcAndInstruction.instruction

            val targets = project.resolveAllMethodReferences(declaringClass, name, desc)

            if (targets.size == 1) {
                val invokedMethod = targets.head
                val declIntf = invokedMethod.classFile
                if (declIntf.isInterfaceDeclaration &&
                    classHierarchy.allSuperinterfacetypes(declaringClass).exists { sintf =>
                        val sintfCfO = project.classFile(sintf)
                        sintfCfO.isDefined &&
                            sintfCfO.get.findMethod(name, desc).exists(_.isStatic) &&
                            classHierarchy.allSuperinterfacetypes(sintf).contains(declIntf.thisType)
                    }) {

                    val pc = pcAndInstruction.pc
                    val l = InstructionLocation(methodLocation, pc)

                    instructionsLocations(0) += l
                }
            }
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }
}
