package org.opalj.fpcf.fixtures.immutability.sandbox50;

import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;

import java.util.Date;

public class Test {
    @EffectivelyNonAssignableField("")
    protected Date d;
}
