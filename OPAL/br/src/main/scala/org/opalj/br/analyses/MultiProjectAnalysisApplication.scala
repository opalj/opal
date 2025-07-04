/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.util.PerformanceEvaluation.time
import org.rogach.scallop.ScallopConf

import java.io.File
import java.util.Calendar
import scala.language.implicitConversions

/**
 * Default command-line runner implementation for analyses to be applied to one or more projects
 *
 * @author Dominik Helm
 */
abstract class MultiProjectAnalysisApplication {

    type ConfigType <: ScallopConf with MultiProjectAnalysisConfig[ConfigType]

    def evaluate(
                    cp:             Iterable[File],
                    analysisConfig: ConfigType
                ): Unit

    def createConfig(args: Array[String]): ConfigType

    def main(args: Array[String]): Unit = {

        val analysisConfig: ConfigType = createConfig(args)

        val begin = Calendar.getInstance()
        Console.println(begin.getTime)

        time {
            analysisConfig.foreachProject(evaluate)
        }(t => println("evaluation time: " + t.toSeconds))

        val end = Calendar.getInstance()
        Console.println(end.getTime)
    }

}
