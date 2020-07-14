package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

import java.util.Random;

public class NotDeterministicLazyIntInstantiation {
    @LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
    private int r;

    public int getR(){
                if(r==0){
                    r = new Random().nextInt();
                }
                return r;
    }
}
