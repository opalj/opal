package org.opalj.fpcf.fixtures.immutability.reference;

import org.opalj.fpcf.fixtures.immutability.classes.generic.TrivialMutableClass;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceEscapesAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceNotEscapesAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

public class Escapers{

}

class TransitiveEscape1 {
    @ImmutableReferenceEscapesAnnotation("")
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
    @ImmutableReferenceEscapesAnnotation("")
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
class NotEscape {
    @ImmutableReferenceNotEscapesAnnotation("")
    private TrivialMutableClass tmc1 = new TrivialMutableClass();

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
