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

import java.io.File
import java.net.URL

/**
 * Defines implicit conversions to wrap some types of analyses such that they generate 
 * results of type [[de.tud.cs.st.bat.resolved.analyses.ReportableAnalysisResult]].
 * 
 * @author Michael Eichberg
 */
package object analyses {

    import language.implicitConversions

    /**
     * Converts a URL in a location identifier.
     */
    def urlToLocationIdentifier(url: URL): String = {
        if (url.getProtocol() == "file")
            url.getFile()
        else
            "!URL<"+url.toExternalForm()+">"
    }

    def fileToLocationIdentifier(file: File): String = file.getAbsolutePath()

    /**
     * An analysis that may produce a result.
     */
    type SingleOptionalResultAnalysis[-Source, +AnalysisResult] = Analysis[Source, Option[AnalysisResult]]

    /**
     * An analysis that may produce multiple results. E.g., an analysis that looks for
     * instances of bug patterns.
     */
    type MultipleResultsAnalysis[-Source, +AnalysisResult] = Analysis[Source, Iterable[AnalysisResult]]

    implicit def fileBasedAnalysisToAnalysisWithReportableResults(
        analysis: Analysis[File, Iterable[SourceLocationBasedReport[File]]]): Analysis[File, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[File, Iterable[SourceLocationBasedReport[File]]](
            analysis,
            results ⇒ results.map(_.consoleReport(fileToLocationIdentifier)).mkString("\n")
        )
    }

    implicit def fileBasedAnalysisWithOptionalResultToAnalysisWithReportableResults(
        analysis: Analysis[File, Option[SourceLocationBasedReport[File]]]): Analysis[File, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[File, Option[SourceLocationBasedReport[File]]](
            analysis,
            results ⇒ results.map(_.consoleReport(fileToLocationIdentifier)).mkString("\n")
        )
    }

    implicit def fileBasedAnalysisToAnalysisWithReportableResult(
        analysis: Analysis[File, SourceLocationBasedReport[File]]): Analysis[File, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[File, SourceLocationBasedReport[File]](
            analysis,
            results ⇒ results.consoleReport(fileToLocationIdentifier)
        )
    }

    implicit def urlBasedReportableAnalysesToAnalysisWithReportableResults(
        analysis: Analysis[URL, Iterable[ReportableAnalysisResult]]): Analysis[URL, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[URL, Iterable[ReportableAnalysisResult]](
            analysis,
            results ⇒ results.map(_.consoleReport).mkString("\"")
        )
    }

    implicit def urlBasedAnalysisToAnalysisWithReportableResults(
        analysis: Analysis[URL, Iterable[SourceLocationBasedReport[URL]]]): Analysis[URL, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[URL, Iterable[SourceLocationBasedReport[URL]]](
            analysis,
            results ⇒ results.map(_.consoleReport(urlToLocationIdentifier)).mkString("\n")
        )
    }

    implicit def urlBasedAnalysisToAnalysisWithReportableResult(
        analysis: Analysis[URL, SourceLocationBasedReport[URL]]): Analysis[URL, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[URL, SourceLocationBasedReport[URL]](
            analysis,
            result ⇒ result.consoleReport(urlToLocationIdentifier)
        )
    }

    implicit def urlBasedAnalysisWithOptionalResultToAnalysisWithReportableResults(
        analysis: Analysis[URL, Option[SourceLocationBasedReport[URL]]]): Analysis[URL, ReportableAnalysisResult] = {

        new ReportableAnalysisAdapter[URL, Option[SourceLocationBasedReport[URL]]](
            analysis,
            results ⇒ results.map(_.consoleReport(urlToLocationIdentifier)).mkString("\n")
        )
    }

}




