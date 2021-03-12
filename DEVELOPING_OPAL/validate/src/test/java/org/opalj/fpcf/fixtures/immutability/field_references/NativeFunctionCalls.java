package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class NativeFunctionCalls {

    @MutableField("concrete class type is deep immutable")
    @MutableFieldReference("field is final")
    private Object finalObjectField = new Object();

    @MutableField("The field objectField has a non final reassignable reference")
    @MutableFieldReference("There is a native function in that class")
    private Object objectField = new Object();

    native void hulului();

}
