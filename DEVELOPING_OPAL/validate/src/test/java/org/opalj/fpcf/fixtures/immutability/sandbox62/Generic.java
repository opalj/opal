package org.opalj.fpcf.fixtures.immutability.sandbox62;

import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.DependentImmutableClass;

@DependentImmutableClass("")
public class Generic<T> {
    final T t;
    public Generic(T t){
        this.t = t;
    }
}

@DeepImmutableClass("")
final class FinalEmptyClass {}


@DeepImmutableClass("")
class Extend extends Generic<FinalEmptyClass> {

    public Extend(FinalEmptyClass fec){
        super(fec);
    }
}
