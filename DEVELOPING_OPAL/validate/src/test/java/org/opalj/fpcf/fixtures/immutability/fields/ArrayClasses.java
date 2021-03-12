/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;

public class ArrayClasses {
/*
    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is eager initialized")
    private Object[] zzz = new Object[]{1, 2, 3};

    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("Array is initialized in the constructor")
    private Object[] a;

    public ArrayClasses() {
        a = new Object[]{5, 6, 7, 8};
    }

    @ShallowImmutableField("The elements of the array are manipulated after initialization and can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] b = new Object[]{1, 2, 3, 4, 5};

    public Object[] getB() {
        b[2] = 2;
        return b;
    }

    @MutableField("Array has a mutable reference.")
    @MutableFieldReference("The array is initalized always when the InitC function is called")
    private Object[] c;

    public void InitC() {
        c = new Object[]{1, 2, 3};
    }


    @ShallowImmutableField("The elements of the array can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] d = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getD() {
        return d;
    }


    @ShallowImmutableField("The elements of the array can escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized lazily.")
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


    @MutableField("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeFieldReference("The array is initialized lazily but not thread safe.")
    private Object[] f;

    public void getF() {
        if (f == null) {
            f = new Object[]{1, 2, 3};
        }
    }


    @ShallowImmutableField("One element of the array is written after initialization.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized lazily and thread safe.")
    private Object[] g;

    public synchronized void getG() {
        if (g == null)
            g = new Object[]{1, 2, 4, 5};
        g[2] = 2;
        //return g;
    }


    @DeepImmutableField("The elements of the array can not escape.")
    @ImmutableFieldReference("The array is initialized eagerly.")
    private Object[] h = new Object[]{new Object(), new Object(), new Object()};


    @ShallowImmutableField("The elements of the array can escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized thread safe and eagerly.")
    private Object[] i;

    public synchronized Object[] getI(){
        if(i==null)
            i = new Object[]{new Object(), new Object(), new Object()};
        return i;
    }


    @DeepImmutableField("The elements of the array can not escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized thread safen and eagerly.")
    private Object[] j;

    public synchronized void getJ(){ //Object[]
        if(j==null)
            j = new Object[]{new Object(), new Object(), new Object()};
        //j[2] = new Object();
        // return j;
    }


    @DeepImmutableField("The elements of the array can not escape")
    @ImmutableFieldReference("The array is not initialized.")
    private Object[] k;


    @MutableField("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeFieldReference("The array is initialized lazily but not thread-safe.")
    private int[] m;

     public int[] getM() {
       if(m==null)
            m = new int[]{1,2,3};
        return m;
    }


    @MutableField("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeFieldReference("The array is initialized lazily but not thread-safe.")
    private Object[] n;

    public Object[] getN(){ //Object[]
        if(n==null)
            n = new Object[]{new Object(), new Object(), new Object()};
        n[2] = new Object();
         return n;
    }


    @ShallowImmutableField("The elements of the array can escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized thread safe and lazily.")
    private Object[] o;

    public synchronized Object[] getO(){
        if(o==null)
            o = new Object[]{new Object(), new Object(), new Object()};
        o[2] = new Object();
        return o;
    }

    @ShallowImmutableField("One element of the array can escape")
    @LazyInitializedThreadSafeFieldReference("The array is thread safe lazily intialized.")
    private Object[] p;
    public synchronized Object getP(){
        if(p==null)
            p = new Object[]{new Object(), new Object(), new Object()};
        return p[2];
    }

    //TODO @DeepImmutableField("The elements of the array can escape, but have a deep immutable reference.")
    //TODO @LazyInitializedThreadSafeFieldReference("The array is thread safe lazily intialized.")
    private Integer[] q;
    public synchronized Integer getQ(){
        if(q==null)
            q = new Integer[]{new Integer(1), new Integer(2), new Integer(3)};
        return q[2];
    }
*/
}
