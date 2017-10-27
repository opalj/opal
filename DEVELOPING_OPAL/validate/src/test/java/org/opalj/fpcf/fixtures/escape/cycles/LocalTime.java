package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

public class LocalTime implements TemporalAccessor {
    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field.isTimeBased();
        }

        return field != null && field.isSupportedBy(this);
    }

}
