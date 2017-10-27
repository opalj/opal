package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.*;

public class YearMonth implements Temporal {

    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field == YEAR || field == MONTH_OF_YEAR ||
                    field == PROLEPTIC_MONTH || field == YEAR_OF_ERA || field == ERA;
        }
        return field != null && field.isSupportedBy(this);
    }
}
