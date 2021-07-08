/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;
//import edu.cmu.cs.glacier.qual.Immutable;

import java.util.*;

//@Immutable
public class DifferentLazyInitializedFieldTypes {

    //@Immutable
     @TransitivelyImmutableField("Lazy initialized field with primitive type.")
     @LazilyInitializedField("field is thread safely lazy initialized")
    private int inTheGetterLazyInitializedIntField;

    public synchronized int getInTheGetterLazyInitializedIntField(){
        if(inTheGetterLazyInitializedIntField ==0)
            inTheGetterLazyInitializedIntField = 5;
        return inTheGetterLazyInitializedIntField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private Integer inGetterSynchronizedSimpleLazyInitializedIntegerField;

    public synchronized Integer getInGetterSynchronizedSimpleLazyInitializedIntegerField(){
        if(inGetterSynchronizedSimpleLazyInitializedIntegerField==0)
            inGetterSynchronizedSimpleLazyInitializedIntegerField = 5;
        return inGetterSynchronizedSimpleLazyInitializedIntegerField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private double inGetterSynchronizedLazyInitializedDoubleField;

    public synchronized double getD3(){
        if(inGetterSynchronizedLazyInitializedDoubleField==0d)
            inGetterSynchronizedLazyInitializedDoubleField = 5;
        return inGetterSynchronizedLazyInitializedDoubleField;
    }

    //@Immutable
    @TransitivelyImmutableField("field has an immutable reference and a deep immutable type")
    @LazilyInitializedField("field is in a synchronized getter lazy initialized")
    private Double inAGetterLazyInitializedObjectDoubleField;

    public synchronized Double getInAGetterLazyInitializedObjectDoubleField(){
        if(inAGetterLazyInitializedObjectDoubleField==0)
            inAGetterLazyInitializedObjectDoubleField = 5d;
        return inAGetterLazyInitializedObjectDoubleField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private float inGetterSynchronizedLazyInitializedFloatField;

    public synchronized float getf3(){
        if(inGetterSynchronizedLazyInitializedFloatField==0)
            inGetterSynchronizedLazyInitializedFloatField = 5f;
        return inGetterSynchronizedLazyInitializedFloatField;
    }

    //@Immutable
    @TransitivelyImmutableField("field has an immutable field reference and a deep immutable type")
    @LazilyInitializedField("the field is in a getter thread safely lazy initialized")
    private float inAGetterLazyInitializedFloatObjectField;

    public synchronized Float getInAGetterLazyInitializedFloatObjectField(){
        if(inAGetterLazyInitializedFloatObjectField==0)
            inAGetterLazyInitializedFloatObjectField = 5f;
        return inAGetterLazyInitializedFloatObjectField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private byte inGetterSynchronizedLazyInitializedByteField;

    public synchronized byte getInGetterSynchronizedLazyInitializedByteField(){
        if(inGetterSynchronizedLazyInitializedByteField==0)
            inGetterSynchronizedLazyInitializedByteField = 5;
        return inGetterSynchronizedLazyInitializedByteField;
    }

    //@Immutable
    @TransitivelyImmutableField("field has a primitive type and is synchronized lazy initialized")
    @LazilyInitializedField("field is thread safely lazy initialized in a getter")
    private Byte inAGetterLazyInitializedByteObjectField;

    public synchronized Byte getInAGetterLazyInitializedByteObjectField(){
        if(inAGetterLazyInitializedByteObjectField==0)
            inAGetterLazyInitializedByteObjectField = 5;
        return inAGetterLazyInitializedByteObjectField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private char inGetterSynchronizedLazyInitializedCharField;

    public synchronized char c3(){
        if(inGetterSynchronizedLazyInitializedCharField == '\u0000')
            inGetterSynchronizedLazyInitializedCharField = 5;
        return inGetterSynchronizedLazyInitializedCharField;
    }

    //@Immutable
    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private long inGetterSynchronizedLazyInitializedLongField;

    public synchronized long getInGetterSynchronizedLazyInitializedLongField(){
        if(inGetterSynchronizedLazyInitializedLongField == 0l)
            inGetterSynchronizedLazyInitializedLongField = 5;
        return inGetterSynchronizedLazyInitializedLongField;
    }

    //@Immutable
    @TransitivelyImmutableField("")
    @LazilyInitializedField("")
    private Long lO3;

    public synchronized Long lO3(){
        if(lO3 == 0l)
            lO3 = 5l;
        return lO3;
    }


    @TransitivelyImmutableField("The concrete type of the object that is assigned is known")
    @LazilyInitializedField("lazy initialized within a synchronized method")
    private String inAGetterLazyInitializedString;

    public synchronized String getInAGetterLazyInitializedString(){
        if(inAGetterLazyInitializedString == null)
            inAGetterLazyInitializedString = "abc";
        return inAGetterLazyInitializedString;
    }


    @TransitivelyImmutableField("The concrete type of the object that is assigned is known")
    @LazilyInitializedField("lazy initialized within a synchronized method")
    private Object inAGetterLazyInitializedObjectReference;

    public synchronized Object getInAGetterLazyInitializedObjectReference(){
        if(inAGetterLazyInitializedObjectReference == null)
            inAGetterLazyInitializedObjectReference = new Object();
        return inAGetterLazyInitializedObjectReference;
    }

    @NonTransitivelyImmutableField("concrete assigned object is known but manipulation of the referenced object with .add")
    @LazilyInitializedField("thread safe lazy initialization due to synchronized method")
    private List<Object> lazyInitializedLinkedListWithManipulationAfterwards;

    public synchronized List<Object> initLinkedList3(){
        if(lazyInitializedLinkedListWithManipulationAfterwards == null)
            lazyInitializedLinkedListWithManipulationAfterwards = new LinkedList<Object>();
        lazyInitializedLinkedListWithManipulationAfterwards.add(new Object());
        return lazyInitializedLinkedListWithManipulationAfterwards;
    }

    @NonTransitivelyImmutableField("immutable reference but assigned object escapes via getter")
    @LazilyInitializedField("synchronized lazy initialization")
    private Set<Object> inTheGetterLazyInitializedSet;

    public synchronized Set<Object> getInTheGetterLazyInitializedSet(){
        if(inTheGetterLazyInitializedSet == null)
            inTheGetterLazyInitializedSet = new HashSet<Object>();
        inTheGetterLazyInitializedSet.add(new Object());
        return inTheGetterLazyInitializedSet;
    }

    @NonTransitivelyImmutableField("immutable reference but assigned object escapes via getter")
    @LazilyInitializedField("synchronized lazy initialization")
    private HashMap<Object, Object> inTheGetterLazyInitializedHashMap;

    public synchronized HashMap<Object, Object> getInTheGetterLazyInitializedHashMap(){
        if(inTheGetterLazyInitializedHashMap == null)
            inTheGetterLazyInitializedHashMap = new HashMap<Object, Object>();
        inTheGetterLazyInitializedHashMap.put(new Object(), new Object());
        return inTheGetterLazyInitializedHashMap;
    }
}
