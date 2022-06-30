/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq

import org.opalj.value.KnownTypedValue
import org.opalj.da.ClassFile
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.tac.DUVar
import org.opalj.tac.LazyTACUsingAIKey
import org.opalj.tac.TACode

/**
 * Groups test case features that perform classloading.
 *
 * @note The features represent the __Classloading__ test cases from the Call Graph Test Project
 *       (JCG).
 *
 * @author Dominik Helm
 */
class Classloading(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    type V = DUVar[KnownTypedValue]

    // required types and descriptors
    val ClassLoaderT = ObjectType("java/lang/ClassLoader")

    val loadClassMD = MethodDescriptor(ObjectType.String, ObjectType.Class)

    override def featureIDs: Seq[String] = {
        Seq(
            "CL1+CL2+CL3", /* 0 --- Standard java classloaders */
            "CL4" /* 1 --- Custom classloaders */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        implicit val locations: Array[LocationsContainer[S]] =
            Array.fill(featureIDs.size)(new LocationsContainer[S])

        val tacai = project.get(LazyTACUsingAIKey)

        val classHierarchy = project.classHierarchy

        val hasCustomClassLoaders =
            project.allClassFiles exists { cf =>
                classHierarchy.isSubtypeOf(cf.thisType, ClassLoaderT) &&
                    !(cf.thisType.fqn.startsWith("java/") ||
                        cf.thisType.fqn.startsWith("sun/") ||
                        cf.thisType.fqn.startsWith("com/sun") ||
                        cf.thisType.fqn.startsWith("javax/management/") ||
                        cf.thisType.fqn.startsWith("jdk/nashorn/internal/runtime/"))
            }

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case i @ INVOKEVIRTUAL(declClass, "loadClass", loadClassMD) if classHierarchy.isSubtypeOf(declClass, ClassLoaderT) => i
            }: PartialFunction[Instruction, Instruction])
            TACode(_, stmts, pcToIndex, _, _) = tacai(method)
        } {
            val pc = pcAndInvocation.pc
            val l = InstructionLocation(methodLocation, pc)

            if (hasCustomClassLoaders)
                locations(1) += l // potential custom classloader
            else
                locations(0) += l // standard java classloader
        }

        ArraySeq.unsafeWrapArray(locations)
    }

    def isStandardClassLoader(receiverType: ReferenceType): Boolean = {
        receiverType.asObjectType.fqn.startsWith("java/")
    }
}
