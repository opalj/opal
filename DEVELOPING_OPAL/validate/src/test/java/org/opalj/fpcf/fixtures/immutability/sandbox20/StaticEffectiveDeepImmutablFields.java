package org.opalj.fpcf.fixtures.immutability.sandbox20;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;

import java.io.IOException;

public class StaticEffectiveDeepImmutablFields {
    //@DeepImmutableField("")
    //private static Object o = new Object();

    //@DeepImmutableField("")
    //private static Object[] objectArray = new ;

    @ShallowImmutableField("")
    private static MutableClass mc = new MutableClass();


    @ShallowImmutableField("")
    private final int[] terminationLock = new int[0];

    /*static {
        objectArray = new Object[]{new Object(), new Object()};
    }*/
    int n = 10;

    public Object get(){
        synchronized (this.terminationLock){
        n = 5;
        }
        return identity(mc);
    }

    //javax.security.auth.login.LoginContext.PARAMS
    //com.sun.jmx.remote.internal.ServerNotifForwarder
    //sun.misc.SoftCache.entrySet

    public Object identity(Object o){
        return o;
    }

    private int checkState() throws IOException {
        int n = 10;
        synchronized(this.terminationLock) {
            n = 5;
        }
        return n;
    }

}

class MutableClass {
    public int n = 5;
}


