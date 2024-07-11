/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package interpretation

trait SoundnessMode {

    def isHigh: Boolean
}

object SoundnessMode {

    def apply(high: Boolean): SoundnessMode = if (high) HighSoundness else LowSoundness
}

object LowSoundness extends SoundnessMode {

    override def isHigh: Boolean = false
}

object HighSoundness extends SoundnessMode {

    override def isHigh: Boolean = true
}
