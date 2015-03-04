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
package org.opalj
package bugpicker
package core
package analysis

import scala.Console.{ RED, YELLOW, RESET }

/**
 * Describes the overall relevance of a finding.
 *
 * When calculating the relevance you should take all
 * properties of the associated issue into consideration:
 *  - kind of issue
 *  - category of issue
 *  - accuracy of the analysis
 *
 * @param value A value between 0 (undetermined), 1 (not really relevant) and
 *      100 (absolutely relevant).
 *
 * @author Michael Eichberg
 */
final case class Relevance(value: Int) extends AnyVal {

    def merge(other: Relevance): Relevance =
        new Relevance(if (this.value > other.value) this.value else other.value)

    /**
     * The lower the value, the "whiter" the color. If the value is 100
     * then the color will be black.
     */
    def asHTMLColor = {
        val rgbValue = 0 + (100 - value) * 2
        s"rgb($rgbValue,$rgbValue,$rgbValue)"
    }

    def asAnsiColoredString: String = {
        if (value > 65)
            RED+"[error]"+RESET
        else if (value > 32)
            YELLOW+"[warn]"+RESET
        else
            "[info]"
    }

    def asEclipseConsoleString: String = {
        s"[relevance=$value]"
    }
}

object Relevance {
    final val OfUtmostRelevance = Relevance(99)
    final val VeryHigh = Relevance(80)
    final val High = Relevance(70)
    final val DefaultRelevance = Relevance(50)
    final val Low = Relevance(30)
    final val VeryLow = Relevance(10)
    final val CommonIdiom = Relevance(3)
    final val ProvenAssertion = Relevance(2)
    final val OfNoRelevance = Relevance(1)
    final val TechnicalArtifact = OfNoRelevance

    final val Undetermined = Relevance(0)
}
