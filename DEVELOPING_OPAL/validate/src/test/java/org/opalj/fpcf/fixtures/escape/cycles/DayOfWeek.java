package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.DAY_OF_WEEK;

public enum DayOfWeek implements TemporalAccessor {

    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field == DAY_OF_WEEK;
        }

        return field != null && field.isSupportedBy(this);

    }
}
