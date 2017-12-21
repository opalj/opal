package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

public class LocalTime implements TemporalAccessor {
    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {

        if (field instanceof ChronoField) {
            return field.isTimeBased();
        }

        return field != null && field.isSupportedBy(this);
    }

}
