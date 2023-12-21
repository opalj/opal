/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

import org.opalj.br.analyses.Project
import org.opalj.da.ClassFile

/**
 * Test case feature where two methods are defined a class that do only vary in the specified return
 * type. This is not possible on Java source level both is still valid in bytecode.
 *
 * @author Michael Reif
 */
class NonJavaBytecode2(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: Seq[String] = {
        Seq("NJ2")
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
        } {
            val methodMap = mutable.Map.empty[String, Int]
            classFile.methods.filterNot(_.isSynthetic).foreach { m =>
                val jvmDescriptor = m.descriptor.toJVMDescriptor
                val mdWithoutReturn = jvmDescriptor.substring(0, jvmDescriptor.lastIndexOf(')') + 1)
                val key = m.name + mdWithoutReturn

                val prev = methodMap.getOrElse(key, 0)
                methodMap.put(key, prev + 1)
            }

            val hasCase = methodMap.values.exists(_ >= 2)

            if (hasCase)
                instructionsLocations(0) += classFileLocation
        }

        ArraySeq.unsafeWrapArray(instructionsLocations)
    }
}
