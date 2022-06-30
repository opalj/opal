/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

import org.opalj.da.ClassFile
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction

/**
 * Groups test case features that perform a polymorphic method calls over package boundaries. This is
 * particulary relevant for package visible types and/or package visible methods.
 *
 * @note The features represent the __PackageBoundaries__ test cases from the Call Graph Test Project (JCG).
 *
 * @author Michael Reif
 */
class PackageBoundaries(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq(
            "PB1", /* 0 --- calls are not allowed to leave the package */
            "PB2" /* 1 --- TODO */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val instructionsLocations = Array.fill(2)(new LocationsContainer[S])

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            callerType = classFile.thisType
            callerPackage = callerType.packageName
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInvocation <- body collect ({
                case iv: INVOKEVIRTUAL => iv
            }: PartialFunction[Instruction, INVOKEVIRTUAL])
        } {
            val pc = pcAndInvocation.pc
            val invoke = pcAndInvocation.value

            val l = InstructionLocation(methodLocation, pc)

            val receiver = invoke.declaringClass
            val name = invoke.name
            val methodDescriptor = invoke.methodDescriptor

            if (receiver.isObjectType && (receiver.asObjectType.packageName eq callerPackage)) {
                val rtOt = receiver.asObjectType
                val rcf = project.classFile(rtOt)

                val isPackageVisibleMethod = rcf.map {
                    _.findMethod(name, methodDescriptor).map(_.isPackagePrivate).getOrElse(false)
                }.getOrElse(false)

                val matchesPreconditions = isPackageVisibleMethod &&
                    isMethodOverriddenInDiffPackage(
                        callerPackage,
                        rtOt,
                        name,
                        methodDescriptor,
                        project
                    )

                if (matchesPreconditions) {
                    if (project.classHierarchy.existsSubclass(rtOt, project) { cf =>
                        val ot = cf.thisType
                        if (ot.packageName eq callerPackage) {
                            isMethodOverriddenInDiffPackage(rtOt, ot, name, methodDescriptor, project)
                        } else false
                    }) {
                        instructionsLocations(1) += l
                        1 /* it exists a subtype `S` within the same package as the declared typed `D`
                        that inherits transitively from type `D`, such that `S` <: `S'` <: `D` where
                        `S'` overrides the package private method `D.m` */
                    } else {
                        // it exists an target in another package that overrides the method
                        instructionsLocations(0) += l
                    }
                }
            }
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }

    private def isMethodOverriddenInDiffPackage[S](
        callerPackage:    String,
        rtOt:             ObjectType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        project:          Project[S]
    ) = {
        project.classHierarchy.existsSubclass(rtOt, project) { sot =>
            (sot.thisType.packageName ne callerPackage) &&
                sot.findMethod(name, methodDescriptor).map(_.isPackagePrivate).getOrElse(false)
        }
    }

    private[this] def isMethodOverriddenInDiffPackage[S](
        declaredType:     ObjectType,
        targetType:       ObjectType,
        name:             String,
        methodDescriptor: MethodDescriptor,
        project:          Project[S]
    ): Boolean = {

        val classHierarchy = project.classHierarchy
        val callingPackage = declaredType.packageName

        val worklist = ArrayBuffer[Int]()
        classHierarchy.directSupertypes(targetType).foreach { sot =>
            worklist.append(sot.id)
        }
        while (worklist.nonEmpty) {
            val cur = worklist.remove(0)
            val ot = classHierarchy.getObjectType(cur)
            if (ot.packageName ne callingPackage) {
                val cf = project.classFile(ot).get
                val mo = cf.findMethod(name, methodDescriptor)
                if (mo.isDefined && mo.get.isPackagePrivate) {
                    return true;
                }
            }
            classHierarchy.directSupertypes(ot).foreach { sot =>
                worklist.append(sot.id)
            }
        }

        false
    }
}
