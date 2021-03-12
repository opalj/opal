package org.opalj.fpcf.fixtures.immutability.sandbox50;

import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

import java.util.Date;

public class Test {
    @ImmutableFieldReference("")
    protected Date d;
}
