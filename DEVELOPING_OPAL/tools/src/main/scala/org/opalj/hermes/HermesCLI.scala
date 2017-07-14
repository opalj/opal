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

import java.io.File

/**
 * Executes all analyses to determine the representativeness of the given projects.
 *
 * @author Michael Eichberg
 */
object HermesCLI {

    object Hermes extends HermesCore {

        override def updateProjectData(f: ⇒ Unit): Unit = Hermes.synchronized { f }

        override def reportProgress(f: ⇒ Double): Unit = Hermes.synchronized { f }
    }

    def main(args: Array[String]): Unit = {

        var configFile: String = null
        var csvFile: String = null

        var i = 0
        while (i < args.length) {
            args(i) match {
                case "--csv" ⇒
                    i += 1; csvFile = args(i)
                case s ⇒ configFile = s
            }
            i += 1
        }
        if (configFile == null || csvFile == null) {
            import Console.err
            err.println("OPAL - Hermes")
            err.println("Parameters 1) the configuration which lists a corpus' projects")
            err.println("           2) the file to which the results should be exported")
            err.println("              --csv=<FileName>")
            err.println()
            err.println("java org.opalj.hermes.HermesCLI <ConfigFile.json> --csv=<FileName>")
            System.exit(1)
        }

        Hermes.analysesFinished onChange { (_, _, isFinished) ⇒
            if (isFinished) {
                Hermes.exportCSV(new File(csvFile))
                System.exit(0)
            }
        }
        Hermes.initialize(new File(configFile))
        Hermes.analyzeCorpus(runAsDaemons = false)
    }

}
