/*
 * BSD 2-Clause License:
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
package util

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.collection.immutable.Chain
import org.opalj.da.ClassFile

import scalafx.application.Platform

/**
 * @author Michael Reif
 */
trait APIFeatureExtractor extends FeatureExtractor {

    def apiFeatures: Chain[APIFeature]

    /**
     * The unique ids of the extracted features.
     */
    override def featureIDs: Seq[String] = apiFeatures.map(_.toFeatureID).toSeq

    /**
     * The major function which analyzes the project and extracts the feature information.
     *
     * @note '''Every query should regularly check that its thread is not interrupted!''' E.g.,
     *       using `Thread.currentThread().isInterrupted()`.
     */
    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)],
        features:             Map[String, Feature[S]]
    ): Unit = {

        var invocationCounts = apiFeatures.foldLeft(Map.empty[String, Int])(
            (result, feature) ⇒ result + ((feature.toFeatureID, 0))
        )

        for {
            cf ← project.allClassFiles
            MethodWithBody(code) ← cf.methods
            apiFeature ← apiFeatures
            featureID = apiFeature.toFeatureID
            APIMethod(declClass, name, descriptor, isStatic) ← apiFeature.getAPIMethods
            inst ← code.instructions if inst.isInstanceOf[MethodInvocationInstruction]
            if !isInterrupted()
        } yield {

            val foundCall = inst match {
                case INVOKESTATIC(`declClass`, _, `name`, `descriptor`) if isStatic ⇒ true
                case INVOKEVIRTUAL(`declClass`, `name`, `descriptor`) if !isStatic ⇒ true
                case INVOKESPECIAL(`declClass`, _, `name`, `descriptor`) if !isStatic ⇒ true
                case _ ⇒ false
            }

            if (foundCall) {
                val count = invocationCounts.get(featureID).get + 1
                invocationCounts = invocationCounts + ((featureID, count))
            }
        }

        if (!isInterrupted) { // we want to avoid that we return partial results
            Platform.runLater {
                apiFeatures.foreach { apiMethod ⇒
                    val featureID = apiMethod.toFeatureID
                    features(featureID).count.value = invocationCounts.get(featureID).get
                }
            }
        }
    }
}
