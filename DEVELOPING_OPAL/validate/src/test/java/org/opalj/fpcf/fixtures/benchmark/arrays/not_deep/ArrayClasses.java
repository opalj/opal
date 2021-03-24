/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.arrays.not_deep;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.MutableType;

//@Immutable
@MutableType("")
@MutableClass("")
public class ArrayClasses<T> {

    //@Immutable
    @ShallowImmutableField("The elements of the array are manipulated after initialization and can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] b = new Object[]{1, 2, 3, 4, 5};

    public Object[] getB() {
        b[2] = 2;
        return b;
    }

    //@Immutable
    @MutableField("Array has a mutable reference.")
    @MutableFieldReference("The array is initalized always when the InitC function is called")
    private Object[] c;

    public void InitC() {
        c = new Object[]{1, 2, 3};
    }

    //@Immutable
    @ShallowImmutableField("The elements of the array can escape.")
    @ImmutableFieldReference("The array is eager initialized.")
    private Object[] d = new Object[]{1, 2, 3, 4, 5,};

    public Object[] getD() {
        return d;
    }

    //@Immutable
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

    //@Immutable
    @MutableField("The reference is seen as mutable.")
    @LazyInitializedNotThreadSafeFieldReference("The array is initialized lazily but not thread safe.")
    private Object[] f;

    public void getF() {
        if (f == null) {
            f = new Object[]{1, 2, 3};
        }
    }

    //@Immutable
    @ShallowImmutableField("One element of the array is written after initialization.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized lazily and thread safe.")
    private Object[] g;

    public synchronized void getG() {
        if (g == null)
            g = new Object[]{1, 2, 4, 5};
        g[2] = 2;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    //@Immutable
    @MutableField("")
    @LazyInitializedNotThreadSafeFieldReference("The array is not thread-safe lazily initialized.")
    private int[] m;

     public int[] getM() {
       if(m==null)
            m = new int[]{1,2,3};
        return m;
    }

    //@Immutable
    @MutableField("")
    @LazyInitializedNotThreadSafeFieldReference("The array is not thread-safe lazily initialized.")
    private Object[] n;

    public Object[] getN(){ //Object[]
        if(n==null)
            n = new Object[]{new Object(), new Object(), new Object()};
        n[2] = new Object();
         return n;
    }

    //@Immutable
    @ShallowImmutableField("The elements of the array can escape.")
    @LazyInitializedThreadSafeFieldReference("The array is initialized thread safe and lazily.")
    private Object[] o;

    public synchronized Object[] getO(){
        if(o==null)
            o = new Object[]{new Object(), new Object(), new Object()};
        o[2] = new Object();
        return o;
    }

    //@Immutable
    @ShallowImmutableField("One element of the array can escape")
    @LazyInitializedThreadSafeFieldReference("The array is thread safe lazily intialized.")
    private Object[] p;
    public synchronized Object getP(){
        if(p==null)
            p = new Object[]{new Object(), new Object(), new Object()};
        return p[2];
    }

    //@Immutable
    @ShallowImmutableField("escaping")
    private String[] stringArray;

    //@Immutable
    @ShallowImmutableField("escaping")
    private int[] intArray;

    //@Immutable
    @ShallowImmutableField("escaping")
    private Object[] oArr;

    //@Immutable
    @ShallowImmutableField("escaping")
    private T[] tArray;

    ArrayClasses(String[] stringArray, int[] intArray, Object[] oArr, T[] tArray) {
        this.stringArray = stringArray;
        this.intArray = intArray;
        this.oArr = oArr;
        this.tArray = tArray;
    }
}
