package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.MutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.MutableReferenceAnnotation;

public class ArrayClasses {

    @DeepImmutableFieldAnnotation("The elements of the array can not escape")
    @ImmutableReferenceAnnotation("Array is eager initialized")
    private Object[] zzz = new Object[]{1, 2, 3};

    @DeepImmutableFieldAnnotation("The elements of the array can not escape")
    @ImmutableReferenceAnnotation("Array is initialized in the constructor")
    private Object[] a;

    public ArrayClasses() {
        a = new Object[]{5, 6, 7, 8};
    }

    @ShallowImmutableFieldAnnotation("The elements of the array are manipulated after initialization and can escape.")
    @ImmutableReferenceAnnotation("The array is eager initialized.")
    private Object[] b = new Object[]{1, 2, 3, 4, 5};

    public Object[] getB() {
        b[2] = 2;
        return b;
    }

    @MutableFieldAnnotation("Array has a mutable reference.")
    @MutableReferenceAnnotation("The array is initalized always when the InitC function is called")
    private Object[] c;

    public void InitC() {
        c = new Object[]{1, 2, 3};
    }


    @ShallowImmutableFieldAnnotation("The elements of the array can escape.")
    @ImmutableReferenceAnnotation("The array is eager initialized.")
    private Object[] d = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getD() {
        return d;
    }


    @ShallowImmutableFieldAnnotation("The elements of the array can escape.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is initialized lazily.")
    private Object[] e;

    public synchronized Object[] getE() {
        Object[] tmp;
        if (e == null) {
            tmp = new Object[3];
            for (int i = 0; i < 3; i++)
                tmp[i] = i;
            this.e = tmp;
        }
        return this.e;
    }


    @MutableFieldAnnotation("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeReferenceAnnotation("The array is initialized lazily but not thread safe.")
    private Object[] f;

    public void getF() {
        if (f == null) {
            f = new Object[]{1, 2, 3};
        }
    }


    @ShallowImmutableFieldAnnotation("One element of the array is written after initialization.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is initialized lazily and thread safe.")
    private Object[] g;

    public synchronized void getG() {
        if (g == null)
            g = new Object[]{1, 2, 4, 5};
        g[2] = 2;
        //return g;
    }


    @DeepImmutableFieldAnnotation("The elements of the array can not escape.")
    @ImmutableReferenceAnnotation("The array is initialized eagerly.")
    private Object[] h = new Object[]{new Object(), new Object(), new Object()};


    @ShallowImmutableFieldAnnotation("The elements of the array can escape.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is initialized thread safe and eagerly.")
    private Object[] i;

    public synchronized Object[] getI(){
        if(i==null)
            i = new Object[]{new Object(), new Object(), new Object()};
        return i;
    }


    @DeepImmutableFieldAnnotation("The elements of the array can not escape.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is initialized thread safen and eagerly.")
    private Object[] j;

    public synchronized void getJ(){ //Object[]
        if(j==null)
            j = new Object[]{new Object(), new Object(), new Object()};
        //j[2] = new Object();
        // return j;
    }


    @DeepImmutableFieldAnnotation("The elements of the array can not escape")
    @ImmutableReferenceAnnotation("The array is not initialized.")
    private Object[] k;


    @MutableFieldAnnotation("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeReferenceAnnotation("The array is initialized lazily but not thread-safe.")
    private int[] m;

     public int[] getM() {
       if(m==null)
            m = new int[]{1,2,3};
        return m;
    }


    @MutableFieldAnnotation("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeReferenceAnnotation("The array is initialized lazily but not thread-safe.")
    private Object[] n;

    public Object[] getN(){ //Object[]
        if(n==null)
            n = new Object[]{new Object(), new Object(), new Object()};
        n[2] = new Object();
         return n;
    }


    @ShallowImmutableFieldAnnotation("The elements of the array can escape.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is initialized thread safe and lazily.")
    private Object[] o;

    public synchronized Object[] getO(){
        if(o==null)
            o = new Object[]{new Object(), new Object(), new Object()};
        o[2] = new Object();
        return o;
    }

    @ShallowImmutableFieldAnnotation("One element of the array can escape")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is thread safe lazily intialized.")
    private Object[] p;
    public synchronized Object getP(){
        if(p==null)
            p = new Object[]{new Object(), new Object(), new Object()};
        return p[2];
    }

    @DeepImmutableFieldAnnotation("The elements of the array can escape, but have a deep immutable reference.")
    @LazyInitializedThreadSafeReferenceAnnotation("The array is thread safe lazily intialized.")
    private Integer[] q;
    public synchronized Integer getQ(){
        if(q==null)
            q = new Integer[]{new Integer(1), new Integer(2), new Integer(3)};
        return q[2];
    }

}
