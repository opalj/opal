package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

public class ChronoLocalDateTimeImpl implements ChronoLocalDateTime {

    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {
        if (field instanceof ChronoField) {
            ChronoField f = (ChronoField) field;
            return f.isDateBased() || f.isTimeBased();
        }
        return field != null && field.isSupportedBy(this);
    }

}
