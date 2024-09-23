/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.analyses.SomeProject
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.logOnce

/**
 * Shared config between the multiple analyses of the string analysis package.
 *
 * @author Maximilian Rüsch
 */
trait StringAnalysisConfig {

    val project: SomeProject
    implicit def logContext: LogContext

    private final val ConfigLogCategory = "analysis configuration - string analysis - universal"

    implicit val highSoundness: Boolean = {
        val isHighSoundness =
            try {
                project.config.getBoolean(StringAnalysisConfig.HighSoundnessConfigKey)
            } catch {
                case t: Throwable =>
                    logOnce {
                        Error(ConfigLogCategory, s"couldn't read: ${StringAnalysisConfig.HighSoundnessConfigKey}", t)
                    }
                    false
            }

        logOnce(Info(ConfigLogCategory, s"using ${if (isHighSoundness) "high" else "low"} soundness mode"))
        isHighSoundness
    }
}

object StringAnalysisConfig {

    final val HighSoundnessConfigKey = "org.opalj.fpcf.analyses.string.highSoundness"
}
