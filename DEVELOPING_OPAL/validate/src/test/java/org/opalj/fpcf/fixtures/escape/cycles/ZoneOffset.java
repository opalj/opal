package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.DAY_OF_MONTH;
import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.MONTH_OF_YEAR;

public final class ZoneOffset implements TemporalAccessor {

    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field == MONTH_OF_YEAR || field == DAY_OF_MONTH;
        }

        return field != null && field.isSupportedBy(this);
    }
}
