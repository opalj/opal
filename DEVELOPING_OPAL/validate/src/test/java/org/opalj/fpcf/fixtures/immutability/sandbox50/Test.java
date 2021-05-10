package org.opalj.fpcf.fixtures.immutability.sandbox50;

import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;

import java.util.Date;

public class Test {
    @NonAssignableFieldReference("")
    protected Date d;
}
