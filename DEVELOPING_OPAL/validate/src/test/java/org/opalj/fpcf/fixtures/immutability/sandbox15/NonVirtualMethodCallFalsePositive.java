package org.opalj.fpcf.fixtures.immutability.sandbox15;

import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;

public class NonVirtualMethodCallFalsePositive {

    public float n = 5;

    private float byteValue(){
        return n;
    }

    //@ImmutableFieldReference("")
    private NonVirtualMethodCallFalsePositive instance;
    public synchronized  NonVirtualMethodCallFalsePositive getInstance(){
        NonVirtualMethodCallFalsePositive tmpInstance = this.instance;
        boolean b = tmpInstance.byteValue() == 0;
        if(b==true){
           instance = tmpInstance = new NonVirtualMethodCallFalsePositive();
        }
        return instance;
    }

}

class Test{
    int n;
    public Test(int n){
        this.n = n;
    }
    /*sun.util.locale.provider.DictionaryBasedBreakIterator
    sun.util.calendar.ZoneInfoFile.ruleArray
    com.sun.media.sound.SoftEnvelopeGenerator.on
    java.awt.Container.EMPTY_ARRAY */
    //sun.awt.AppContext.threadAppContext
}
