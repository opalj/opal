package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.DAY_OF_MONTH;
import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.MONTH_OF_YEAR;

public final class MonthDay implements TemporalAccessor {

    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {
        if (field instanceof ChronoField) {
            return field == MONTH_OF_YEAR || field == DAY_OF_MONTH;
        }

        return field != null && field.isSupportedBy(this);

    }

}
