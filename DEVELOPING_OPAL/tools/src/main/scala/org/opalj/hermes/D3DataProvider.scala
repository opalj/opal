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
package org.opalj.hermes

import java.net.URL

import play.api.libs.json.JsObject
import play.api.libs.json.Json

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scalafx.collections.ObservableBuffer

/**
 * Transforms feature query results into JSON-formatted string values
 */
class D3DataProvider(
        private val featureMatrix:   ObservableBuffer[ProjectFeatures[URL]],
        private val selection:       SelectionOption,
        private val optionSelection: mutable.HashMap[String, Boolean]
) {

    def getSelectedProjects(
        accCountThreshold:  Int,
        accSingleDisplayed: Double,
        accTotalDisplayed:  Double
    ): JsObject = {
        val projectSelection: SelectionOption = selection.find("projects").get
        val statisticSelection: SelectionOption = selection.find("statistics").get
        val featureSelection: SelectionOption = selection.find("features").get
        val projects = ArrayBuffer.empty[JsObject]
        featureMatrix.filter(p ⇒ projectSelection.find(p.id.value).isDefined).foreach { project ⇒
            // reformat for sorting
            val statistics: Seq[(String, Double)] = project.projectConfiguration.statistics.toSeq
            val features: Seq[(String, Double)] = project.features.map(f ⇒ {
                (f.value.id, f.value.count.asInstanceOf[Double])
            })
            val selectedValues = (statistics ++ features)
                .filter(f ⇒ statisticSelection.find(f._1).isDefined
                    || featureSelection.find(f._1).isDefined)
                .sortBy(_._2).reverse

            val children = ArrayBuffer.empty[JsObject]
            val total: Double = selectedValues.map(_._2).sum

            if (accCountThreshold > 0 && selectedValues.length > accCountThreshold) {
                var sum: Double = 0
                var rest: Double = 0
                val restData = ArrayBuffer.empty[JsObject]
                selectedValues foreach { value ⇒
                    if ((children.length < 2) || (value._2 >= total * accSingleDisplayed)
                        && (sum + value._2 <= total * accTotalDisplayed)) {
                        children += Json.obj(
                            "id" → value._1,
                            "value" → value._2
                        )
                        sum += value._2
                    } else {
                        restData += Json.obj(
                            "id" → value._1.replace('\n', ' '),
                            "value" → value._2
                        )
                    }
                }
                rest = total - sum
                if (rest > 0) {
                    children += Json.obj(
                        "id" → "<rest>",
                        "value" → rest,
                        "metrics" → Json.toJson(restData.toSeq)
                    )
                }
            } else {
                selectedValues foreach { value ⇒
                    children += Json.obj(
                        "id" → value._1,
                        "value" → value._2
                    )
                }
            }

            projects += Json.obj(
                "id" → project.id.value,
                "value" → total,
                "metrics" → Json.toJson(children.toSeq)
            )
        }
        Json.obj(
            "id" → "flare",
            "options" → Json.toJson(optionSelection),
            "children" → Json.toJson(projects.toSeq)
        )
    }

    def getSingleProject(projectName: String): JsObject = {
        val statisticSelection: SelectionOption = selection.find("statistics").get
        val featureSelection: SelectionOption = selection.find("features").get
        val project = featureMatrix.find(_.id.value == projectName).get
        val statistics: Seq[(String, Double)] = project.projectConfiguration.statistics.toSeq
        val features: Seq[(String, Double)] = project.features.map(f ⇒ {
            (f.value.id, f.value.count.asInstanceOf[Double])
        })
        val selectedValues = (statistics ++ features)
            .filter(f ⇒ statisticSelection.find(f._1).isDefined
                || featureSelection.find(f._1).isDefined)

        val children = ArrayBuffer.empty[JsObject]
        selectedValues foreach { value ⇒
            children += Json.obj(
                "id" → value._1,
                "value" → value._2
            )
        }
        Json.obj(
            "id" → projectName,
            "options" → Json.toJson(optionSelection),
            "children" → Json.toJson(children.toSeq)
        )
    }
}
