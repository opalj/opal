package org.opalj.fpcf.fixtures.field_immutability;

import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;

public class protectedClass_shallow {
    protected TrivialMutableClass tmc = new TrivialMutableClass();
}
