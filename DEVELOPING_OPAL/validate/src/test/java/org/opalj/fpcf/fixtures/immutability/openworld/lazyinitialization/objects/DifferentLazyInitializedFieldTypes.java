/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.lazyinitialization.objects;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;

import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazilyInitializedField;

/**
 * This class encompasses different cases of lazily initialized fields.
 */
public class DifferentLazyInitializedFieldTypes {

    @TransitivelyImmutableField("Lazy initialized field with primitive type.")
    @LazilyInitializedField("field is thread safely lazy initialized")
    private int inTheGetterLazyInitializedIntField;

    public synchronized int getInTheGetterLazyInitializedIntField(){
        if(inTheGetterLazyInitializedIntField ==0)
            inTheGetterLazyInitializedIntField = 5;
        return inTheGetterLazyInitializedIntField;
    }

    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private Integer inGetterSynchronizedSimpleLazyInitializedIntegerField;

    public synchronized Integer getInGetterSynchronizedSimpleLazyInitializedIntegerField(){
        if(inGetterSynchronizedSimpleLazyInitializedIntegerField==0)
            inGetterSynchronizedSimpleLazyInitializedIntegerField = 5;
        return inGetterSynchronizedSimpleLazyInitializedIntegerField;
    }

    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @LazilyInitializedField("lazy initialization in a synchronized getter method")
    private long inGetterSynchronizedLazyInitializedLongField;

    public synchronized long getInGetterSynchronizedLazyInitializedLongField(){
        if(inGetterSynchronizedLazyInitializedLongField == 0l)
            inGetterSynchronizedLazyInitializedLongField = 5;
        return inGetterSynchronizedLazyInitializedLongField;
    }

    @TransitivelyImmutableField("The field is not assignable and has a primitive value")
    @LazilyInitializedField("The field is assigned deterministic with always the same value")
    private Long lazyintializedLongObject;

    public synchronized Long getLongObject(){
        if(lazyintializedLongObject == 0l)
            lazyintializedLongObject = 5l;
        return lazyintializedLongObject;
    }

    @TransitivelyImmutableField("The concrete type of the object that is assigned is known")
    @LazilyInitializedField("lazy initialized within a synchronized method")
    private String inAGetterLazyInitializedString;

    public synchronized String getInAGetterLazyInitializedString(){
        if(inAGetterLazyInitializedString == null)
            inAGetterLazyInitializedString = "abc";
        return inAGetterLazyInitializedString;
    }

    @NonTransitivelyImmutableField("")
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
