/* License (BSD Style License):
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
package findrealbugs
package analyses

import resolved._
import resolved.analyses._
import resolved.instructions._
import ai._
import ai.domain.l1._

/**
 * Can be used to extend a domain to record a list of useless operations,
 * such as `x | 0` (is always `x`) or `x | -1` (is always `-1`).
 *
 * @author Daniel Klauer
 */
private trait RecordBitNops extends PreciseIntegerValues[Method] {
    /**
     * Helper class to group a program counter and message string for a list of reports.
     */
    class PCReport(val pc: PC, val message: String)

    /**
     * List of reports recorded during the domain evaluation.
     */
    var reports: List[PCReport] = List.empty

    /**
     * Add a report to the recorder's list of reports.
     *
     * @param pc The program counter to report.
     * @param message The message string to report.
     */
    private def addReport(pc: PC, message: Option[String]) = {
        if (message.isDefined) {
            reports = new PCReport(pc, message.get) :: reports
        }
    }

    /**
     * DomainValue matcher to allow matching IntegerValues.
     */
    private object IntValue {
        /**
         * unapply() method to allowing this object to be used as pattern matcher.
         *
         * @param value The `DomainValue` to match on.
         * @return Optional integer value stored in the `DomainValue`.
         */
        def unapply(value: DomainValue): Option[Int] = {
            getIntValue[Option[Int]](value)(Some(_))(None)
        }
    }

    /**
     * Overrides the domain's ior() callback in order to look for certain bit OR
     * operations.
     *
     * @param pc The current program counter.
     * @param l The left hand side operand.
     * @param r The right hand side operand.
     * @return The result of the bit OR operation.
     */
    abstract override def ior(pc: PC, l: DomainValue, r: DomainValue): DomainValue = {
        addReport(pc, (l, r) match {
            case (IntValue(0), _) ⇒
                Some("0 | x: bit or operation with 0 left operand is useless")
            case (IntValue(-1), _) ⇒
                Some("-1 | x: bit or operation with -1 left operand always returns -1")
            case (_, IntValue(0)) ⇒
                Some("x | 0: bit or operation with 0 right operand is useless")
            case (_, IntValue(-1)) ⇒
                Some("x | -1: bit or operation with -1 right operand always returns -1")
            case _ ⇒
                None
        })
        super.ior(pc, l, r)
    }

    /**
     * Overrides the domain's iand() callback in order to look for certain bit AND
     * operations.
     *
     * @param pc The current program counter.
     * @param l The left hand side operand.
     * @param r The right hand side operand.
     * @return The result of the bit AND operation.
     */
    abstract override def iand(pc: PC, l: DomainValue, r: DomainValue): DomainValue = {
        addReport(pc, (l, r) match {
            case (IntValue(0), _) ⇒
                Some("0 & x: bit and operation with 0 left operand always returns 0")
            case (IntValue(-1), _) ⇒
                Some("-1 & x: bit and operation with -1 left operand is useless")
            case (_, IntValue(0)) ⇒
                Some("x & 0: bit and operation with 0 right operand always returns 0")
            case (_, IntValue(-1)) ⇒
                Some("x & -1: bit and operation with -1 right operand is useless")
            case _ ⇒
                None
        })
        super.ior(pc, l, r)
    }
}

/**
 * This analysis reports various useless bit operations, for example `x | 0` (is always
 * `x`) or `x | -1` (is always `-1`).
 *
 * @author Daniel Klauer
 */
class BitNops[S]
        extends MultipleResultsAnalysis[S, LineAndColumnBasedReport[S]] {

    def description: String = "Reports various useless bit operations."

    /**
     * Runs this analysis on the given project.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[S],
        parameters: Seq[String] = List.empty): Iterable[LineAndColumnBasedReport[S]] = {

        import AnalysesHelpers.pcToOptionalLineNumber

        // Build a list of reports per method, then flatten that to get the overall list
        // of reports.
        (for {
            classFile ← project.classFiles
            method ← classFile.methods if method.body.isDefined

            // Only run AI if the method body contains the instructions we're looking for.
            //
            // This increases performance when analyzing systems from the Qualitas Corpus,
            // with BitNops, for example:
            //
            // with this check:
            //     ant:    8s
            //   antlr:    7s
            //     aoi:   22s
            // argouml:   44s
            // aspectj: 1m30s
            //   axion:    2s
            //
            // without this check:
            //     ant:  1m30s
            //   antlr:  1m18s
            //     aoi: 15m36s
            // argouml: 21m46s
            // aspectj: 20m24s
            //   axion:    21s
            //
            if method.body.get.instructions.exists {
                case IOR | IAND ⇒ true;
                case _          ⇒ false;
            }
        } yield {
            // Uses a domain with RecordBitNops to find no-ops in the method
            val domain = new DefaultConfigurableDomain(method) with RecordBitNops
            BaseAI(classFile, method, domain)

            // Turn the reports collected by RecordBitNops into
            // LineAndColumnBasedReports
            domain.reports.map(report ⇒
                new LineAndColumnBasedReport(
                    project.source(classFile.thisType),
                    Severity.Info,
                    pcToOptionalLineNumber(method.body.get, report.pc),
                    None,
                    report.message))
        }).flatten
    }
}
