/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat
package findrealbugs

/**
 * `FindRealBugs.analyze()` users can implement this interface in order to obtain
 * information about the progress of the analysis process.
 *
 * TODO [Clarify/Refactor] Implementations of this trait need to be thread safe - the client cannot be effectively shielded from concurrency issues anyway and currently the responsibility is distributed.  
 * 
 * @author Daniel Klauer
 * @author Florian Brandherm
 */
trait ProgressListener {

    /**
     * Override this callback to be notified when a certain analysis is started.
     *
     * Note: Since the analyses are executed in parallel, begin/end events may not
     * necessarily be in order. Calls to this method may come from multiple threads.
     * However, all calls to this method are synchronized.
     *
     * @param name The analysis' name.
     * @param position The analysis' start number. 1st analysis = 1, 2nd analysis = 2,
     * etc.
     */
       // TODO [Refactor/Rename] How about: "startingAnalysis"? Reason: this method does not "begin"/start an analysis. 
    def beginAnalysis(name: String, position: Int) : Unit

    /**
     * Override this callback to be notified when a certain analysis ends.
     *
     * @see [[ProgressListener.beginAnalysis]]
     *
     * @param name The analysis' name.
     * @param position The analysis' start number.
     * @param seconds The time it took for this analysis to run, in seconds.
     * @param reports The reports produced by the analysis, if any.
     */
    // TODO [Refactor/Rename] How about: "analysisCompleted"? Reason: "andAnalysis" does not end an analysis. 
    def endAnalysis(
        name: String,
        position: Int,
        seconds: Double,
        reports: FindRealBugs.AnalysisReports) : Unit
}
