/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedThreadSafeFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

public class EffectivelyImmutableFields {

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
    analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutabiltiy", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is not written after initialization",
    analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private int simpleInitializedFieldWithPrimitiveType = 5;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private int lazyInitializedFieldWithPrimitiveType;

    public synchronized void initLazyInitializedFieldWithPrimitiveType(){
        if(lazyInitializedFieldWithPrimitiveType == 0)
            lazyInitializedFieldWithPrimitiveType = 5;
    }

     @DeepImmutableField("Lazy initialized field with primitive type.")
     @LazyInitializedThreadSafeFieldReference("field is thread safely lazy initialized")
    private int inTheGetterLazyInitializedFieldWithPrimitiveType;

    public synchronized int getInTheGetterLazyInitializedFieldWithPrimitiveType(){
        if(inTheGetterLazyInitializedFieldWithPrimitiveType ==0)
            inTheGetterLazyInitializedFieldWithPrimitiveType = 5;
        return inTheGetterLazyInitializedFieldWithPrimitiveType;
    }

    @DeepImmutableField(value = "immutable reference and deep immutable type",
    analyses = L3FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "effective immutable field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private Integer effectiveImmutableIntegerField = 5;


    @MutableField(value = "due to mutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @MutableFieldReference(value = "write of reference objects is not atomic",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private Integer simpleLazyInitializedIntegerField;

    public void initSimpleLazyInitializedIntegerField(){
        if(simpleLazyInitializedIntegerField==0)
            simpleLazyInitializedIntegerField = 5;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized method")
    private Integer synchronizedSimpleLazyInitializedIntegerField;

    public synchronized void initNO2(){
        if(synchronizedSimpleLazyInitializedIntegerField==0)
            synchronizedSimpleLazyInitializedIntegerField = 5;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private Integer inGetterSynchronizedSimpleLazyInitializedIntegerField;

    public synchronized Integer getInGetterSynchronizedSimpleLazyInitializedIntegerField(){
        if(inGetterSynchronizedSimpleLazyInitializedIntegerField==0)
            inGetterSynchronizedSimpleLazyInitializedIntegerField = 5;
        return inGetterSynchronizedSimpleLazyInitializedIntegerField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutabiltiy", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private double effectiveImmutableDoubleField = 5d;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private double synchronizedLazyInitializedDoubleField;

    public synchronized void initD2(){
        if(synchronizedLazyInitializedDoubleField ==0d)
            synchronizedLazyInitializedDoubleField = 5d;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private double inGetterSynchronizedLazyInitializedDoubleField;

    public synchronized double getD3(){
        if(inGetterSynchronizedLazyInitializedDoubleField==0d)
            inGetterSynchronizedLazyInitializedDoubleField = 5;
        return inGetterSynchronizedLazyInitializedDoubleField;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @ImmutableFieldReference("field is effective immutable")
    private Double effectiveImmutableObjectDoubleField = 5d;

    @DeepImmutableField("field has an immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private Double lazyInitializedObjectDoubleField;

    public synchronized void initLazyInitializedObjectDoubleField(){
        if(lazyInitializedObjectDoubleField==0)
            lazyInitializedObjectDoubleField = 5d;
    }

    @DeepImmutableField("field has an immutable reference and a deep immutable type")
    @LazyInitializedThreadSafeFieldReference("field is in a synchronized getter lazy initialized")
    private Double inAGetterLazyInitializedObjectDoubleField;

    public synchronized Double getInAGetterLazyInitializedObjectDoubleField(){
        if(inAGetterLazyInitializedObjectDoubleField==0)
            inAGetterLazyInitializedObjectDoubleField = 5d;
        return inAGetterLazyInitializedObjectDoubleField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutabiltiy", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is not written after initialization",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private float effectiveImmutableFloatField = 5;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private float synchronizedLazyInitializedFloatField;

    public synchronized void initF2(){
        if(synchronizedLazyInitializedFloatField ==0)
            synchronizedLazyInitializedFloatField = 5f;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private float inGetterSynchronizedLazyInitializedFloatField;

    public synchronized float getf3(){
        if(inGetterSynchronizedLazyInitializedFloatField==0)
            inGetterSynchronizedLazyInitializedFloatField = 5f;
        return inGetterSynchronizedLazyInitializedFloatField;
    }

    @DeepImmutableField("field has an immutable field reference and a deep immutable type")
    @ImmutableFieldReference("the field reference is effective immutable")
    private Float effectiveImmutableFloatObjectField = 5f;

    @DeepImmutableField("field has an immutable field reference and a deep immutable type")
    @LazyInitializedThreadSafeFieldReference("the field is thread safely lazy initialized")
    private Float lazyInitializedFloatObjectField;

    public synchronized void initFO2(){
        if(lazyInitializedFloatObjectField ==0)
            lazyInitializedFloatObjectField = 5f;
    }

    @DeepImmutableField("field has an immutable field reference and a deep immutable type")
    @LazyInitializedThreadSafeFieldReference("the field is in a getter thread safely lazy initialized")
    private float inAGetterLazyInitializedFloatObjectField;

    public synchronized Float getInAGetterLazyInitializedFloatObjectField(){
        if(inAGetterLazyInitializedFloatObjectField==0)
            inAGetterLazyInitializedFloatObjectField = 5f;
        return inAGetterLazyInitializedFloatObjectField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private byte effectiveImmutableByteField = 5;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safely lazy initialized")
    private byte synchronizedLazyInitializedByteField;

    public synchronized void initB2(){
        if(synchronizedLazyInitializedByteField ==0)
            synchronizedLazyInitializedByteField = 5;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private byte inGetterSynchronizedLazyInitializedByteField;

    public synchronized byte getInGetterSynchronizedLazyInitializedByteField(){
        if(inGetterSynchronizedLazyInitializedByteField==0)
            inGetterSynchronizedLazyInitializedByteField = 5;
        return inGetterSynchronizedLazyInitializedByteField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private Byte effectiveImmutableByteObjectField = 5;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safely lazy initialized")
    private Byte lazyInitializedByteObjectField;

    public synchronized void initBO2(){
        if(lazyInitializedByteObjectField ==0)
            lazyInitializedByteObjectField = 5;
    }

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safely lazy initialized in a getter")
    private Byte inAGetterLazyInitializedByteObjectField;

    public synchronized Byte getInAGetterLazyInitializedByteObjectField(){
        if(inAGetterLazyInitializedByteObjectField==0)
            inAGetterLazyInitializedByteObjectField = 5;
        return inAGetterLazyInitializedByteObjectField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is effective immutable",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private char c = 'a';

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private char synchronizedLazyInitializedCharField;

    public synchronized void initC2(){
        if(synchronizedLazyInitializedCharField == '\u0000')
            synchronizedLazyInitializedCharField = 'a';
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private char inGetterSynchronizedLazyInitializedCharField;

    public synchronized char c3(){
        if(inGetterSynchronizedLazyInitializedCharField == '\u0000')
            inGetterSynchronizedLazyInitializedCharField = 5;
        return inGetterSynchronizedLazyInitializedCharField;
    }

    @DeepImmutableField(value = "field value has a primitive type and an immutable field reference",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle transitive immutability",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @MutableField(value = "can not handle effective immutabiltiy", analyses = L0FieldImmutabilityAnalysis.class)
    @ImmutableFieldReference(value = "field is not written after initialization",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private long effectiveImmutableLongField = 5;

    @DeepImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazyInitializedThreadSafeFieldReference("field is thread safe lazy initialized")
    private long sychronizedLazyInitializedLongField;

    public synchronized void initL2(){
        if(sychronizedLazyInitializedLongField == 0l)
            sychronizedLazyInitializedLongField = 5l;
    }

    @DeepImmutableField("immutable reference and deep immutable type")
    @LazyInitializedThreadSafeFieldReference("lazy initialization in a synchronized getter method")
    private long inGetterSynchronizedLazyInitializedLongField;

    public synchronized long getInGetterSynchronizedLazyInitializedLongField(){
        if(inGetterSynchronizedLazyInitializedLongField == 0l)
            inGetterSynchronizedLazyInitializedLongField = 5;
        return inGetterSynchronizedLazyInitializedLongField;
    }

    @DeepImmutableField("")
    @ImmutableFieldReference("")
    private Long lO = 5l;

    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Long lO2;

    public synchronized void initLO2(){
        if(lO2 == 0l)
            lO2 = 5l;
    }

    @DeepImmutableField("")
    @LazyInitializedThreadSafeFieldReference("")
    private Long lO3;

    public synchronized Long lO3(){
        if(lO3 == 0l)
            lO3 = 5l;
        return lO3;
    }

    @DeepImmutableField("The concrete assigned object is known to be deep immutable")
    @ImmutableFieldReference("The field is effective immutable")
    private String effectiveImmutableString = "abc";

    @DeepImmutableField("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeFieldReference("lazy initialized within a synchronized method")
    private String lazyInitializedString;

    public synchronized void initLazyInitializedString(){
        if(lazyInitializedString == null)
            lazyInitializedString = "abc";
    }

    @DeepImmutableField("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeFieldReference("lazy initialized within a synchronized method")
    private String inAGetterLazyInitializedString;

    public synchronized String getInAGetterLazyInitializedString(){
        if(inAGetterLazyInitializedString == null)
            inAGetterLazyInitializedString = "abc";
        return inAGetterLazyInitializedString;
    }

    @DeepImmutableField("The concrete assigned object is known to be deep immutable")
    @ImmutableFieldReference("The field is effective immutable")
    private Object effectiveImmutableObjectReference = new Object();

    @DeepImmutableField("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeFieldReference("lazy initialized within a synchronized method")
    private Object lazyInitializedObjectReference;

    public synchronized void initLazyInitializedObjectReference(){
        if(lazyInitializedObjectReference == null)
            lazyInitializedObjectReference = new Object();
    }

    @DeepImmutableField("The concrete type of the object that is assigned is known")
    @LazyInitializedThreadSafeFieldReference("lazy initialized within a synchronized method")
    private Object inAGetterLazyInitializedObjectReference;

    public synchronized Object getInAGetterLazyInitializedObjectReference(){
        if(inAGetterLazyInitializedObjectReference == null)
            inAGetterLazyInitializedObjectReference = new Object();
        return inAGetterLazyInitializedObjectReference;
    }



    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @ImmutableFieldReference("effective immutable reference")
    private List<Object> effectiveImmutableLinkedList = new LinkedList<Object>();

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private List<Object> lazyInitializedLinkedList;

    public synchronized void initLinkedList2(){
        if(lazyInitializedLinkedList == null)
            lazyInitializedLinkedList = new LinkedList<Object>();
    }

    @ShallowImmutableField("concrete assigned object is known but manipulation of the referenced object with .add")
    @LazyInitializedThreadSafeFieldReference("thread safe lazy initialization due to synchronized method")
    private List<Object> lazyInitializedLinkedListWithManipulationAfterwards;

    public synchronized void initLinkedList3(){
        if(lazyInitializedLinkedListWithManipulationAfterwards == null)
            lazyInitializedLinkedListWithManipulationAfterwards = new LinkedList<Object>();
        lazyInitializedLinkedListWithManipulationAfterwards.add(new Object());
    }

    @DeepImmutableField("The concrete type of the object that is assigned is known " +
            "and no manipulation after assignment")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private Object inTheGetterLazyInitializedlinkedList;

    public synchronized Object getInTheGetterLazyInitializedlinkedList(){
        if(inTheGetterLazyInitializedlinkedList == null)
            inTheGetterLazyInitializedlinkedList = new Object();
        return inTheGetterLazyInitializedlinkedList;
    }

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @ImmutableFieldReference("effective immutable reference")
    private List<Object> effectiveImmutableArrayList = new ArrayList<Object>();

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private List<Object> lazyInitializedArrayList;

    public synchronized void initLazyInitializedArrayList(){
        if(lazyInitializedArrayList == null)
            lazyInitializedArrayList = new ArrayList<Object>();
    }

    @ShallowImmutableField("concrete assigned object is known but manipulation of the referenced object with .add")
    @LazyInitializedThreadSafeFieldReference("thread safe lazy initialization due to synchronized method")
    private List<Object> lazyInitializedArrayListWithManipulationAfterwards;

    public synchronized void initLazyInitializedArrayListWithManipulationAfterwards(){
        if(lazyInitializedArrayListWithManipulationAfterwards == null)
            lazyInitializedArrayListWithManipulationAfterwards = new ArrayList<Object>();
        lazyInitializedArrayListWithManipulationAfterwards.add(new Object());
    }

    @ShallowImmutableField("immutable reference but assigned object escapes via getter")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private List<Object> inTheGetterLazyInitializedArrayList;

    public synchronized List<Object> getInTheGetterLazyInitializedArrayList(){
        if(inTheGetterLazyInitializedArrayList == null)
            inTheGetterLazyInitializedArrayList = new ArrayList<Object>();
        return inTheGetterLazyInitializedArrayList;
    }

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @ImmutableFieldReference("effective immutable reference")
    private Set<Object> effectiveImmutableSet = new HashSet<Object>();

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private Set<Object> lazyInitializedSet;

    public synchronized void initLazyInitializedSet(){
        if(lazyInitializedSet == null)
            lazyInitializedSet = new HashSet<Object>();
    }

    @ShallowImmutableField("concrete assigned object is known but manipulation of the referenced object with .add")
    @LazyInitializedThreadSafeFieldReference("thread safe lazy initialization due to synchronized method")
    private Set<Object> lazyInitializedSetWithManipulationAfterwards;

    public synchronized void initSet3(){
        if(lazyInitializedSetWithManipulationAfterwards == null)
            lazyInitializedSetWithManipulationAfterwards = new HashSet<Object>();
       lazyInitializedSetWithManipulationAfterwards.add(new Object());
    }

    @ShallowImmutableField("immutable reference but assigned object escapes via getter")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private Set<Object> inTheGetterLazyInitializedSet;

    public synchronized Set<Object> getInTheGetterLazyInitializedSet(){
        if(inTheGetterLazyInitializedSet == null)
            inTheGetterLazyInitializedSet = new HashSet<Object>();
        return inTheGetterLazyInitializedSet;
    }

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @ImmutableFieldReference("effective immutable reference")
    private HashMap<Object, Object> effectiveImmutableHashMap = new HashMap<Object, Object>();

    @DeepImmutableField("concrete object that is assigned is known and no other manipulation")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private HashMap<Object, Object> lazyInitializedHashMap;

    public synchronized void initLazyInitializedHashMap(){
        if(lazyInitializedHashMap == null)
            lazyInitializedHashMap = new HashMap<Object, Object>();
    }

    @ShallowImmutableField("concrete assigned object is known but manipulation of the referenced object with .put")
    @LazyInitializedThreadSafeFieldReference("thread safe lazy initialization due to synchronized method")
    private HashMap<Object, Object> lazyInitializedHashMapWithManipulationAfterwards;

    public synchronized void initHashMap3(){
        if(lazyInitializedHashMapWithManipulationAfterwards == null)
            lazyInitializedHashMapWithManipulationAfterwards = new HashMap<Object, Object>();
        lazyInitializedHashMapWithManipulationAfterwards.put(new Object(), new Object());
    }

    @ShallowImmutableField("immutable reference but assigned object escapes via getter")
    @LazyInitializedThreadSafeFieldReference("synchronized lazy initialization")
    private HashMap<Object, Object> inTheGetterLazyInitializedHashMap;

    public synchronized HashMap<Object, Object> getInTheGetterLazyInitializedHashMap(){
        if(inTheGetterLazyInitializedHashMap == null)
            inTheGetterLazyInitializedHashMap = new HashMap<Object, Object>();
        return inTheGetterLazyInitializedHashMap;
    }

}
