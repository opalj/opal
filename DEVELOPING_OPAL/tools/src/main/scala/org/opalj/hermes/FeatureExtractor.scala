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

import scala.io.Source
import scala.io.Codec

import com.github.rjeschke.txtmark.Processor

import org.opalj.io.processSource
import org.opalj.br.analyses.Project
import scalafx.beans.property.ObjectProperty

/**
 * Extracts a feature/a set of closely related features of a given project.
 */
trait FeatureExtractor {

    /**
     * The unique ids of the extracted features.
     */
    def featureIDs: Seq[String]

    private[hermes] def createInitialFeatures[S]: Seq[ObjectProperty[Feature[S]]] = {
        featureIDs.map(fid ⇒ ObjectProperty(new Feature[S](fid)))
    }

    /**
     * A short descriptive name; by default the simple name of the class implementing the feature
     * extractor.
     */
    val id: String = {
        val simpleClassName = this.getClass.getSimpleName
        val dollarPosition = simpleClassName.indexOf('$')
        if (dollarPosition == -1)
            simpleClassName
        else
            simpleClassName.substring(0, dollarPosition)
    }

    /**
     * A explanation of the feature (group) using Markdown as its formatting language.
     *
     * By default the name of the class is used to lookup the resource "className.markdown"
     * which is expected to be found along the extractor.
     */
    private[this] def mdDescription: String = {
        val descriptionResource = id+".markdown"
        val descriptionResourceURL = this.getClass.getResource(descriptionResource)
        try {
            processSource(Source.fromURL(descriptionResourceURL)(Codec.UTF8)) { _.mkString }
        } catch {
            case t: Throwable ⇒ s"Not Available ($descriptionResourceURL; ${t.getMessage})"
        }
    }

    lazy val htmlDescription: String = Processor.process(mdDescription)

    def isInterrupted(): Boolean = Thread.currentThread().isInterrupted()

    /**
     * The major function which analyzes the project and extracts the feature information.
     *
     * @note   '''Every query should regularly check that its thread is not interrupted
     *         using `isInterrupted`'''.
     */
    def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): TraversableOnce[Feature[S]]
}

object Queries {

    final val CSS = getClass().getResource("Queries.css").toExternalForm

}
