package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.class_immutability.DependentImmutableClassAnnotation;
import org.opalj.fpcf.properties.class_immutability.ShallowImmutableClassAnnotation;
import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.DependentImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class Escapers{

}

class TransitiveEscape1 {
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private TrivialMutableClass tmc = new TrivialMutableClass();

    public void printTMC(){
        System.out.println(tmc.name);
    }

    public TrivialMutableClass get(){
        TrivialMutableClass tmc1 = this.tmc;
        return tmc1;
    }
}

class TransitiveEscape2 {
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private TrivialMutableClass tmc = new TrivialMutableClass();

    public void printTMC(){
        System.out.println(tmc.name);
    }

    public TrivialMutableClass get(){
        TrivialMutableClass tmc1 = this.tmc;
        TrivialMutableClass tmc2 = tmc1;
        return tmc2;
    }
}
class OneThatNotEscapesAndOneWithDCL {
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private TrivialMutableClass tmc1 = new TrivialMutableClass();

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private TrivialMutableClass tmc2;

    public TrivialMutableClass set() {
        TrivialMutableClass tmc11 = tmc1;
        TrivialMutableClass tmc22 = tmc2;
        if (tmc11 != null) {
            synchronized (this) {
                if (tmc22 == null) {
                    tmc2 = new TrivialMutableClass();
                }
            }
        }
        return tmc2;
    }
}
class GenericEscapes {
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass sgc;

    public GenericEscapes(TrivialMutableClass tmc){
        sgc = new SimpleGenericClass(tmc);
    }
}


@ShallowImmutableClassAnnotation("")
class GenericEscapesTransitive {
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass gc1;

    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private TrivialMutableClass tmc;

    public GenericEscapesTransitive(TrivialMutableClass tmc){
        this.tmc = tmc;
        gc1 = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyNotAbleToResolve{
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private TrivialMutableClass tmc = new TrivialMutableClass();
    @ShallowImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass sgc;
    public GenericNotEscapesMutualEscapeDependencyNotAbleToResolve() {

        this.sgc = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyAbleToResolve{
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private FinalEmptyClass fec = new FinalEmptyClass();
    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private SimpleGenericClass sgc;
    public GenericNotEscapesMutualEscapeDependencyAbleToResolve() {

        this.sgc = new SimpleGenericClass(this.fec);
    }
}







@DependentImmutableClassAnnotation("")
class SimpleGenericClass<T> {
    @DependentImmutableFieldAnnotation(value = "", genericString = "T")
    private T t;
    SimpleGenericClass(T t){
        this.t = t;
    }
}
