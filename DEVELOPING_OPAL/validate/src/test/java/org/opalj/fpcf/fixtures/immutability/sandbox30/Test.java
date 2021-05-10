package org.opalj.fpcf.fixtures.immutability.sandbox30;

public class Test {
    //sun.awt.ExtendedKeyCodes.regularKeyCodesMap
    /*
    @ShallowImmutableField("")
    private static final int[] notifsInfo;

    static {
        notifsInfo = new int[]{1};
    }

    public int[] getNotificationInfo() {
        return notifsInfo.clone();
    }


    @DeepImmutableField("The elements of the array can escape, but have a deep immutable reference.")
    @LazyInitializedThreadSafeFieldReference("The array is thread safe lazily intialized.")
    private Integer[] q;
    public synchronized Integer getQ(){
        if(q==null)
            q = new Integer[]{new Integer(1), new Integer(2), new Integer(3)};
        return q[2];
    }*/
    //com.sun.xml.internal.bind.v2.model.impl.ElementInfoImpl.adapter

}
