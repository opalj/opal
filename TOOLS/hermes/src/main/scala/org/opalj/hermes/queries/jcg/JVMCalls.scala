/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq

import org.opalj.da.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.VirtualMethodInvocationInstruction

/**
 * Groups test case features for features that explicitly must be modeled to imitate the JVM's
 * behaviour, i.e., callbacks that are registered and then (potentially) called from the JVM.
 *
 * @note The features represent the __JVMCalls__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class JVMCalls(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    val Runtime = ObjectType("java/lang/Runtime")
    val Thread = ObjectType("java/lang/Thread")

    override def featureIDs: Seq[String] = {
        Seq(
            "JVMC1", /* 0 --- Runtime.addShutdownHook */
            "JVMC2", /* 1 --- finalizer */
            "JVMC3", /* 2 --- Thread.start */
            "JVMC4", /* 3 --- Thread.exit */
            "JVMC5" /* 4 ---Thread.setUncaughtExceptionHandler */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        // Setup possible types

        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import project.isProjectType

        val threadSubtypes = allSubtypes(Thread, true).filter(isProjectType)
        val relevantTypes = threadSubtypes + Runtime

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
        } {
            if (method.isNotStatic && method.isPublic && method.name == "finalize") {
                locations(1) += methodLocation
            } else if (method.body.nonEmpty) {
                val body = method.body.get
                val pcAndInvocation = body collect ({ case mii: VirtualMethodInvocationInstruction => mii }: PartialFunction[Instruction, VirtualMethodInvocationInstruction])
                pcAndInvocation.foreach { pcAndInvocation =>
                    val pc = pcAndInvocation.pc
                    val mii = pcAndInvocation.value
                    val declClass = mii.declaringClass
                    if (declClass.isObjectType
                        && relevantTypes.contains(declClass.asObjectType)) {
                        val name = mii.name
                        if (name eq "addShutdownHook") {
                            val instructionLocation = InstructionLocation(methodLocation, pc)
                            locations(0) += instructionLocation
                        } else if (name eq "start") {
                            val instructionLocation = InstructionLocation(methodLocation, pc)
                            locations(2) += instructionLocation
                        } else if (name eq "join") {
                            val instructionLocation = InstructionLocation(methodLocation, pc)
                            locations(3) += instructionLocation
                        } else if (name eq "setUncaughtExceptionHandler") {
                            val instructionLocation = InstructionLocation(methodLocation, pc)
                            locations(4) += instructionLocation
                        }
                    }
                }
            }
        }

        ArraySeq.unsafeWrapArray(locations)
    }
}