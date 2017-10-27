package org.opalj.fpcf.fixtures.escape.cycles;

public interface TemporalField {
    boolean isSupportedBy(TemporalAccessor temporal);

    boolean isDateBased();

    boolean isTimeBased();
}
