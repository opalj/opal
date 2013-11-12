/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package analyses

/**
 * Common trait that analyses should inherit from that want to use the general
 * analysis framework [[de.tud.cs.st.bat.resolved.analyses.AnalysisExecutor]].
 *
 * ==Conceptual Idea==
 * An analysis is basically a mapping of a `Project`'s resources to some result.
 *
 * Each analysis can produce (optionally) some result. E.g., a text describing a
 * scenario that leads to a bug, a graph, a report that identifies a specific
 * line or a combination thereof.
 *
 * However, an analysis should never rely on the location of a resource. If an analysis
 * needs access to further resources, it should use the `Project` class.
 *
 * @see [[de.tud.cs.st.bat.resolved.analyses.SingleOptionalResultAnalysis]]
 * @see [[de.tud.cs.st.bat.resolved.analyses.MultipleResultsAnalysis]]
 * @author Michael Eichberg
 */
trait Analysis[Source /*, SomeProject <: Project[Source]*/ , +AnalysisResult] {
    // TODO Consider to also abstract over projects. This would enable us to specify that some analyses may required, e.g., a Project class that provides more/other information  

    /**
     * Analyzes the given project and reports the result(s).
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = List.empty): AnalysisResult

    /**
     * A textual description of this analysis.
     *
     * The description should discuss:
     *  - the goal of the analysis
     *  - weaknesses of the analysis; i.e., whether the analysis may report false
     *    positives or may not report existing bugs (i.e., whether the analysis is
     *    subject to false negatives.)
     *  - if applicable, it should discuss what the developer could/should do in general
     *    to remedy the situation
     *  - if applicable it should discuss the severeness of the found results. I.e.,
     *    if immediate action is typically required, because a bug was found that will
     *    show up at runtime or whether it is a security bug.
     *  - if applicable it should give an example. I.e., what the expected result is given
     *    a project with certain resources.
     */
    def description: String

    /**
     * The copyright statement which contains less than 124 character and no line-breaks.
     */
    def copyright: String = "See project documentation."

    /**
     * A short descriptive title which should contain less than 64 characters and no
     * line-breaks.
     */
    def title: String = this.getClass().getSimpleName()
}

