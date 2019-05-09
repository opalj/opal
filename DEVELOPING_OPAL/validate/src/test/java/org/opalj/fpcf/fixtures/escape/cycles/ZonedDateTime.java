/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.cycles;

import org.opalj.fpcf.properties.escape.AtMostEscapeInCallee;
import org.opalj.tac.fpcf.analyses.escape.InterProceduralEscapeAnalysis;

/**
 * Example code without functionally taken from:
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/time/ZonedDateTime.java#ZonedDateTime
 *
 * @author Florian Kübler
 */
public class ZonedDateTime implements Temporal, ChronoZonedDateTime {

    @Override
    public boolean isSupported(@AtMostEscapeInCallee(value = "Type is accessible but all methods do not let the field escape", analyses = InterProceduralEscapeAnalysis.class) TemporalField field) {
        return field instanceof ChronoField || (field != null && field.isSupportedBy(this));
    }
}
