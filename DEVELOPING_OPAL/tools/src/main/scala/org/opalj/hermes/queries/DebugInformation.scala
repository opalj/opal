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

import org.opalj.br.analyses.Project
import org.opalj.br.MethodWithBody

/**
 * Classifies class file elements which contain debug information.
 *
 * @note  The "SourceDebugExtension" attribute is ignored as it is generally not used.
 *
 * @author Michael Eichberg
 */
class DebugInformation(implicit hermes: HermesConfig) extends FeatureQuery {

    override def featureIDs: IndexedSeq[String] = {
        IndexedSeq(
            /*0*/ "Class File With\nSource Attribute",
            /*1*/ "Method With\nLine Number Table",
            /*2*/ "Method With\nLocal Variable Table",
            /*3*/ "Method With\nLocal Variable Type Table"
        )
    }

    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]] = {
        val locations = Array.fill(4)(new LocationsContainer[S])

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
        } {
            if (classFile.sourceFile.isDefined) locations(0) += classFileLocation

            for {
                method @ MethodWithBody(body) ← classFile.methods
                methodLocation = MethodLocation(classFileLocation, method)
            } {
                if (body.localVariableTable.isDefined) locations(1) += methodLocation
                if (body.localVariableTypeTable.nonEmpty) locations(2) += methodLocation
                if (body.lineNumberTable.isDefined) locations(3) += methodLocation
            }
        }

        for { (locations, index) ← locations.iterator.zipWithIndex } yield {
            Feature[S](featureIDs(index), locations)
        }
    }
}
