/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis;

import java.lang.annotation.*;

/**
 * @author Maximilian Rüsch
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface AllowedSoundnessModes {

    SoundnessMode[] value();
}
