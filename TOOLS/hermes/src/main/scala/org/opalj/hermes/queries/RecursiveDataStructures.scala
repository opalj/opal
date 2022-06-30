/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.graphs.UnidirectionalGraph
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project

/**
 * Identifies recursive data structures. Such data-structure can often significantly limit
 * the scalability of analyses.
 *
 * @author Michael Eichberg
 */
class RecursiveDataStructures(implicit hermes: HermesConfig) extends FeatureQuery {

    override def featureIDs: IndexedSeq[String] = {
        IndexedSeq(
            /*0*/ "Self-recursive Data Structure",
            /*1*/ "Mutually-recursive Data Structure\n2 Types",
            /*2*/ "Mutually-recursive Data Structure\n3 Types",
            /*3*/ "Mutually-recursive Data Structure\n4 Types",
            /*4*/ "Mutually-recursive Data Structure\nmore than 4 Types"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(da.ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        import project.classHierarchy.getObjectType

        val g = new UnidirectionalGraph(ObjectType.objectTypesCount)()

        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        // 1. create graph
        for {
            classFile <- project.allProjectClassFiles
            if !isInterrupted()
            classType = classFile.thisType
            field <- classFile.fields
            fieldType = field.fieldType
        } {
            if (fieldType.isObjectType) {
                g add (classType.id, fieldType.asObjectType.id)
            } else if (fieldType.isArrayType) {
                val elementType = fieldType.asArrayType.elementType
                if (elementType.isObjectType) {
                    g add (classType.id, elementType.asObjectType.id)
                }
            }
        }

        // 2. search for strongly connected components
        for {
            scc <- g.sccs(filterSingletons = true)
            if !isInterrupted()
            /* An scc is never empty! */
            sccCategory = Math.min(scc.size, 5) - 1
            objectTypeID <- scc
            objectType = getObjectType(objectTypeID)
        } {
            locations(sccCategory) += ClassFileLocation(project, objectType)
        }

        for { (locations, index) <- locations.iterator.zipWithIndex } yield {
            Feature[S](featureIDs(index), locations)
        }
    }
}
