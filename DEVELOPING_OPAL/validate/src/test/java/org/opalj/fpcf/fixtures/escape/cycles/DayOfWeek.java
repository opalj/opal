package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

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
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field == DAY_OF_WEEK;
        }

        return field != null && field.isSupportedBy(this);

    }
}
