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
 * @author Maximilian Rüsch
 */
trait UniversalStringConfig {

    val project: SomeProject
    implicit def logContext: LogContext

    private final val ConfigLogCategory = "analysis configuration - string analysis - universal"

    implicit val soundnessMode: SoundnessMode = {
        val mode =
            try {
                SoundnessMode(project.config.getBoolean(UniversalStringConfig.SoundnessModeConfigKey))
            } catch {
                case t: Throwable =>
                    logOnce {
                        Error(ConfigLogCategory, s"couldn't read: ${UniversalStringConfig.SoundnessModeConfigKey}", t)
                    }
                    SoundnessMode(false)
            }

        logOnce(Info(ConfigLogCategory, "using soundness mode " + mode))
        mode
    }
}

object UniversalStringConfig {

    final val SoundnessModeConfigKey = "org.opalj.fpcf.analyses.string.highSoundness"
}
