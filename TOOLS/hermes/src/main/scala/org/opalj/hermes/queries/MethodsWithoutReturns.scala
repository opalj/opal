/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.analyses.Project
import org.opalj.br.instructions.ReturnInstruction
import org.opalj.br.cfg.CFGFactory

/**
 * Counts the number of methods without regular returns.
 *
 * @author Michael Eichberg
 */
class MethodsWithoutReturns(implicit hermes: HermesConfig) extends FeatureQuery {

    final val AlwaysThrowsExceptionMethodsFeatureId = "Never Returns Normally"
    final val InfiniteLoopMethodsFeatureId = "Method with Infinite Loop"
    override val featureIDs: List[String] = List(
        AlwaysThrowsExceptionMethodsFeatureId,
        InfiniteLoopMethodsFeatureId
    )

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {
        val infiniteLoopMethods: LocationsContainer[S] = new LocationsContainer[S]
        val alwaysThrowsExceptionMethods: LocationsContainer[S] = new LocationsContainer[S]

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted
            classFileLocation = ClassFileLocation(source, classFile)
            method ← classFile.methods
            body ← method.body
            hasReturnInstruction = body.exists { (pc, i) ⇒ i.isInstanceOf[ReturnInstruction] }
            if !hasReturnInstruction
        } {
            val cfg = CFGFactory(body, project.classHierarchy)
            if (cfg.abnormalReturnNode.predecessors.isEmpty)
                infiniteLoopMethods += MethodLocation(classFileLocation, method)
            else
                alwaysThrowsExceptionMethods += MethodLocation(classFileLocation, method)
        }

        List(
            Feature[S](AlwaysThrowsExceptionMethodsFeatureId, alwaysThrowsExceptionMethods),
            Feature[S](InfiniteLoopMethodsFeatureId, infiniteLoopMethods)
        )
    }
}
