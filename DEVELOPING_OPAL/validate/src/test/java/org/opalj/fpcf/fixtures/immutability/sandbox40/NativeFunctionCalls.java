package org.opalj.fpcf.fixtures.immutability.sandbox40;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class NativeFunctionCalls {

    @MutableField("concrete class type is deep immutable")
    @MutableFieldReference("field is final")
    private Object finalObjectField = new Object();

    native void hulului();
}