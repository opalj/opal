package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.fpcf.properties.field_immutability.DeepImmutableFieldAnnotation;
import org.opalj.fpcf.properties.field_immutability.ShallowImmutableFieldAnnotation;
import org.opalj.fpcf.properties.reference_immutability.ImmutableReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedThreadSafeReferenceAnnotation;

import java.util.*;

public class EffectivelyImmutableFields {

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private int n1 = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private int n2;

    public synchronized  void initN2(){
        if(n2==0)
            n2 = 5;
    }

     @DeepImmutableFieldAnnotation("")
     @LazyInitializedThreadSafeReferenceAnnotation("")
    private int n3;

    public synchronized int getN3(){
        if(n3==0)
            n3 = 5;
        return n3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Integer nO = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Integer nO2;

    public synchronized void initNO2(){
        if(nO2==0)
            nO2 = 5; //TODO test again
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Integer nO3;

    public synchronized Integer getNO3(){
        if(nO3==0)
            nO3 = 5;
        return nO3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private double d = 5d;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private double d2;

    public synchronized void initD2(){
        if(d2==0d)
            d2 = 5d;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private double d3;

    public synchronized double getD3(){
        if(d3==0d)
            d3 = 5;
        return d3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Double dO = 5d;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Double dO2;

    public synchronized void initDO2(){
        if(dO2==0)
            dO2 = 5d;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Double dO3;

    public synchronized Double getDO3(){
        if(dO3==0)
            dO3 = 5d;
        return dO3;
    }



    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private float  f = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private float f2;

    public synchronized void initF2(){
        if(f2==0)
            f2 = 5f;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private float f3;

    public synchronized float getf3(){
        if(f3==0)
            f3 = 5f;
        return f3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Float fO = 5f;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Float fO2;

    public synchronized void initFO2(){
        if(fO2==0)
            fO2 = 5f;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private float fO3;

    public synchronized Float getfO3(){
        if(fO3==0)
            fO3 = 5f;
        return fO3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private byte b = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private byte b2;

    public synchronized void initB2(){
        if(b2==0)
            b2 = 5;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private byte b3;

    public synchronized byte b3(){
        if(b3==0)
            b3 = 5;
        return b3;
    }



    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Byte b0 = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Byte bO2;

    public synchronized void initBO2(){
        if(bO2==0)
            bO2 = 5;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Byte bO3;

    public synchronized Byte bO3(){
        if(bO3==0)
            bO3 = 5;
        return bO3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private char c = 'a';

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private char c2;

    public synchronized void initC2(){
        if(c2 == '\u0000')
            c2 = 'a';
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private char c3;

    public synchronized char c3(){
        if(c3 == '\u0000')
            c3 = 5;
        return c3;
    }



//------------------------------------------------------------------------------------------------------

    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private long l = 5;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private long l2;

    public synchronized void initL2(){
        if(l2 == 0l)
            l2 = 5l;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private long l3;

    public synchronized long l3(){
        if(l3 == 0l)
            l3 = 5;
        return l3;
    }





    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Long lO = 5l;

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Long lO2;

    public synchronized void initLO2(){
        if(lO2 == 0l)
            lO2 = 5l;
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Long lO3;

    public synchronized Long lO3(){
        if(lO3 == 0l)
            lO3 = 5l;
        return lO3;
    }

//--------------------------------------------------------------------------------------------------------


    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private String s = "abc";

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private String s2;

    public synchronized void initS2(){
        if(s2 == null)
            s2 = "abc";
    }

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private String s3;

    public synchronized String getS3(){
        if(s3 == null)
            s3 = "abc";
        return s3;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Object o = new Object();

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Object o2;

    public synchronized void initO2(){
        if(o2 == null)
            o2 = new Object();
    }

    @DeepImmutableFieldAnnotation("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Object o3;

    public synchronized Object getO3(){
        if(o3 == null)
            o3 = new Object();
        return o3;
    }





    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private List<Object> linkedList = new LinkedList<Object>();

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private List<Object> linkedList2;

    public synchronized void initLinkedList2(){
        if(linkedList2 == null)
            linkedList2 = new LinkedList<Object>();
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private List<Object> linkedList3;

    public synchronized void initLinkedList3(){
        if(linkedList3 == null)
            linkedList3 = new LinkedList<Object>();
        linkedList3.add(new Object());
    }

    @DeepImmutableFieldAnnotation("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Object linkedList4;

    public synchronized Object getLinkedList4(){
        if(linkedList4 == null)
            linkedList4 = new Object();
        return linkedList4;
    }




    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private List<Object> arrayList = new ArrayList<Object>();

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private List<Object> arrayList2;

    public synchronized void initArrayList2(){
        if(arrayList2 == null)
            arrayList2 = new ArrayList<Object>();
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private List<Object> arrayList3;

    public synchronized void initArrayList3(){
        if(arrayList3 == null)
            arrayList3 = new ArrayList<Object>();
        arrayList3.add(new Object());
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private List<Object> arrayList4;

    public synchronized List<Object> getArrayList4(){
        if(arrayList4 == null)
            arrayList4 = new ArrayList<Object>();
        return arrayList4;
    }



    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private Set<Object> set = new HashSet<Object>();

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Set<Object> set2;

    public synchronized void initSet2(){
        if(set2 == null)
            set2 = new HashSet<Object>();
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Set<Object> set3;

    public synchronized void initSet3(){
        if(set3 == null)
            set3 = new HashSet<Object>();
       set3.add(new Object());
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private Set<Object> set4;

    public synchronized Set<Object> getSet4(){
        if(set4 == null)
            set4 = new HashSet<Object>();
        return set4;
    }





    @DeepImmutableFieldAnnotation("")
    @ImmutableReferenceAnnotation("")
    private HashMap<Object, Object> hashMap = new HashMap<Object, Object>();

    @DeepImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private HashMap<Object, Object> hashMap2;

    public synchronized void initHashMap2(){
        if(hashMap2 == null)
            hashMap2 = new HashMap<Object, Object>();
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private HashMap<Object, Object> hashMap3;

    public synchronized void initHashMap3(){
        if(hashMap3 == null)
            hashMap3 = new HashMap<Object, Object>();
        hashMap3.put(new Object(), new Object());
    }

    @ShallowImmutableFieldAnnotation("")
    @LazyInitializedThreadSafeReferenceAnnotation("")
    private HashMap<Object, Object> hashMap4;

    public synchronized HashMap<Object, Object> getHashMap4(){
        if(hashMap4 == null)
            hashMap4 = new HashMap<Object, Object>();
        return hashMap4;
    }

}
