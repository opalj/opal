/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.OFFSET_SECONDS;

/**
 * Example code without functionally taken from:
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/time/OffsetTime.java#OffsetTime
 *
 * @author Florian Kübler
 */
public class OffsetTime implements Temporal {
    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() || field == OFFSET_SECONDS;
        }

        return field != null && field.isSupportedBy(this);
    }
}
