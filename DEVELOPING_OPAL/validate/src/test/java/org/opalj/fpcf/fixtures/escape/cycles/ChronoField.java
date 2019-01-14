/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

/**
 * Example code without functionally taken from:
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/time/temporal/ChronoField.java#ChronoField
 *
 * @author Florian Kübler
 */
public enum ChronoField implements TemporalField {

    NANO_OF_SECOND,

    MICRO_OF_SECOND,

    MILLI_OF_SECOND,

    DAY_OF_WEEK,

    DAY_OF_MONTH,

    MONTH_OF_YEAR,

    PROLEPTIC_MONTH,

    YEAR_OF_ERA,

    YEAR,

    ERA,

    INSTANT_SECONDS,

    OFFSET_SECONDS;

    @Override
    public boolean isSupportedBy(@AtMostEscapeInCallee(
            value = "Type is accessible but all methods do not let the parameter escape",
            analyses = InterProceduralEscapeAnalysis.class) TemporalAccessor temporal
    ) {
        return temporal.isSupported(this);
    }

    @Override
    public boolean isDateBased() {
        return ordinal() < DAY_OF_WEEK.ordinal();
    }

    @Override
    public boolean isTimeBased() {
        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }
}
