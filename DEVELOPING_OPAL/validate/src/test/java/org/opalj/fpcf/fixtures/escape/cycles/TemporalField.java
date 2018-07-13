/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape.cycles;

/**
 * Example code without functionally taken from:
 * http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/time/temporal/TemporalField.java#TemporalField
 *
 * @author Florian Kübler
 */
public interface TemporalField {
    boolean isSupportedBy(TemporalAccessor temporal);

    boolean isDateBased();

    boolean isTimeBased();
}
