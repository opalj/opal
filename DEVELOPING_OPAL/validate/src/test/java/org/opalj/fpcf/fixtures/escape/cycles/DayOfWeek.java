/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.DAY_OF_WEEK;

/**
 * Example code without functionally taken from:
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/time/DayOfWeek.java#DayOfWeek
 *
 * @author Florian Kübler
 */
public enum DayOfWeek implements TemporalAccessor {

    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field == DAY_OF_WEEK;
        }

        return field != null && field.isSupportedBy(this);

    }
}
