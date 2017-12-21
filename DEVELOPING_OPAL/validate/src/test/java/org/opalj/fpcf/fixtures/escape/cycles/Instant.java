package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.analyses.escape.InterProceduralEscapeAnalysis;
import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;

import static org.opalj.fpcf.fixtures.escape.cycles.ChronoField.*;

public class Instant implements Temporal {

    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {
        if (field instanceof ChronoField) {
            return field == INSTANT_SECONDS || field == NANO_OF_SECOND || field == MICRO_OF_SECOND || field == MILLI_OF_SECOND;
        }
        return field != null && field.isSupportedBy(this);
    }

}
