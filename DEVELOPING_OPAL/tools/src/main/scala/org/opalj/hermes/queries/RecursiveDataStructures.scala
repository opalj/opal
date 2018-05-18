/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {

        import project.classHierarchy.getObjectType

        val g = new UnidirectionalGraph(ObjectType.objectTypesCount)()

        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        // 1. create graph
        for {
            classFile ← project.allProjectClassFiles
            if !isInterrupted()
            classType = classFile.thisType
            field ← classFile.fields
            fieldType = field.fieldType
        } {
            if (fieldType.isObjectType) {
                g += (classType.id, fieldType.asObjectType.id)
            } else if (fieldType.isArrayType) {
                val elementType = fieldType.asArrayType.elementType
                if (elementType.isObjectType) {
                    g += (classType.id, elementType.asObjectType.id)
                }
            }
        }

        // 2. search for strongly connected components
        for {
            scc ← g.sccs(filterSingletons = true)
            if !isInterrupted()
            /* An scc is never empty! */
            sccCategory = Math.min(scc.size, 5) - 1
            objectTypeID ← scc
            objectType = getObjectType(objectTypeID)
        } {
            locations(sccCategory) += ClassFileLocation(project, objectType)
        }

        for { (locations, index) ← locations.iterator.zipWithIndex } yield {
            Feature[S](featureIDs(index), locations)
        }
    }
}
