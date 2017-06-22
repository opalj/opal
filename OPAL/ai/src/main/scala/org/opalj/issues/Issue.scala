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
package issues

import scala.xml.Node
import scala.xml.Group
import scala.xml.Unparsed

import play.api.libs.json.Json
import play.api.libs.json.JsValue

/**
 * Describes some issue found in source code.
 *
 * @param analysis The unique id of the analysis.
 *
 * @param relevance The relevance of the issue.
 *
 * @param summary The issue in one short sentence (no line breaks)!
 *
 * @param categories A string that uses small letters and which describes the category of the issue.
 *          The category basically describes '''the property of the software that is
 *          affected ''' by this issue (see [[IssueCategory]] for further details).
 *
 * @param kinds A string that uses small letters and which describes the kind of the issue.
 *          The kind describes how '''this issue manifests itself in the source code'''
 *          (see [[IssueKind]] for further details).
 *
 * @param locations The source code locations related to this issue. This seq must not be empty!
 *
 * @author Michael Eichberg
 */
case class Issue(
        analysis:   String,
        relevance:  Relevance,
        summary:    String,
        categories: Set[String],
        kinds:      Set[String],
        locations:  Seq[IssueLocation],
        details:    Traversable[IssueDetails] = Nil
) extends IssueRepresentations {

    assert(!summary.contains('\n'), s"the summary must not contain new lines:\n$summary")
    assert(locations.nonEmpty, "at least one location must be specified")

    def toXHTML(basicInfoOnly: Boolean): Node = {

        val dataKinds =
            kinds.map(_.replace(' ', '_')).mkString(" ")

        val dataCategories =
            categories.map(_.replace(' ', '_')).mkString(" ")

        <div class="an_issue" style={ s"color:${relevance.toHTMLColor};" } data-relevance={ relevance.value.toString } data-kind={ dataKinds } data-category={ dataCategories }>
            <dl>
                <dt class="analysis">analysis</dt>
                <dd>
                    <span class="analysis_id">{ analysis }</span>
                    |
                    <span class="relevance">relevance={ relevance.value.toString+" ("+relevance.name+")" }</span>
                    |
                    <span class="data_kinds">kind={ kinds.mkString(", ") }</span>
                    |
                    <span class="data_categories">category={ categories.mkString(", ") }</span>
                </dd>
                <dt>summary</dt>
                <dd>
                    <span class="issue_summary">{ Unparsed(summary.replace("\n", "<br>")) }</span>
                </dd>
                { locations.map(_.toXHTML(basicInfoOnly)) }
                {
                    if (!basicInfoOnly && details.nonEmpty)
                        List(
                            <dt>facts</dt>,
                            <dd>
                                { details.map(_.toXHTML(false)) }
                            </dd>
                        )
                    else
                        Group(Nil)
                }
            </dl>
        </div>
    }

    def toAnsiColoredString: String = {
        import Console.{GREEN, RESET}

        val primaryLocation = locations.head
        primaryLocation.toAnsiColoredString+" "+
            relevance.toAnsiColoredString+": "+
            GREEN + primaryLocation.description.map(summary+" - "+_).getOrElse(summary) + RESET
    }

    def toEclipseConsoleString: String = {
        locations.map(_.toEclipseConsoleString).mkString(
            s"$summary«$analysis ${relevance.toEclipseConsoleString}» ", " ", ""
        )
    }

    override def toIDL: JsValue = {
        Json.obj(
            "analysis" → analysis,
            "relevance" → relevance,
            "summary" → summary,
            "categories" → categories,
            "kinds" → kinds,
            "details" → details,
            "locations" → locations
        )
    }
}
