package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterproceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.MaybeEscapeInCallee;

import java.util.HashMap;
import java.util.Map;

public class Parsed implements TemporalAccessor {
    public final Map<TemporalField, Long> fieldValues = new HashMap<>();
    public ChronoLocalDate date;
    public LocalTime time;

    @Override
    public boolean isSupported(@MaybeEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterproceduralEscapeAnalysis.class) TemporalField field) {
        if (fieldValues.containsKey(field) ||
                (date != null && date.isSupported(field)) ||
                (time != null && time.isSupported(field))) {
            return true;
        }

        return field != null && (field instanceof ChronoField == false) &&
                field.isSupportedBy(this);
    }
}
