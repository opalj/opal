package org.opalj.fpcf.fixtures.immutability.sandbox62;

import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;

@DependentlyImmutableClass("")
public class Generic<T> {
    final T t;
    public Generic(T t){
        this.t = t;
    }
}

@TransitivelyImmutableClass("")
final class FinalEmptyClass {}


@TransitivelyImmutableClass("")
class Extend extends Generic<FinalEmptyClass> {

    public Extend(FinalEmptyClass fec){
        super(fec);
    }
}
