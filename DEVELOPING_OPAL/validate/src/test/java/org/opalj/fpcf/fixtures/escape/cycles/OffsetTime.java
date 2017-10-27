package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.OFFSET_SECONDS;

public class OffsetTime implements Temporal {
    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {
        if (field instanceof ChronoField) {
            return field.isTimeBased() || field == OFFSET_SECONDS;
        }

        return field != null && field.isSupportedBy(this);
    }
}
