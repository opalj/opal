package org.opalj.fpcf.fixtures.escape.cycles;

public interface ChronoLocalDateTime extends Temporal, TemporalAccessor {
    @Override
    boolean isSupported(TemporalField field);
}
