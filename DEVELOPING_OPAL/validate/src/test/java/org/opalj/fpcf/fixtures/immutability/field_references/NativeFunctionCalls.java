package org.opalj.fpcf.fixtures.immutability.field_references;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;

public class NativeFunctionCalls {

    @MutableField("concrete class type is deep immutable")
    @AssignableField("field is final")
    private Object finalObjectField = new Object();

    @MutableField("The field objectField has a non final reassignable reference")
    @AssignableField("There is a native function in that class")
    private Object objectField = new Object();

    native void hulului();

}
