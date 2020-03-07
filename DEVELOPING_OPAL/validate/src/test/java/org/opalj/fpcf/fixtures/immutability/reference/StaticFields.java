package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.properties.class_immutability.DeepImmutableClassAnnotation;

@DeepImmutableClassAnnotation("")
public class StaticFields {

    private static String a = "a";
    static String b = "b";

}
