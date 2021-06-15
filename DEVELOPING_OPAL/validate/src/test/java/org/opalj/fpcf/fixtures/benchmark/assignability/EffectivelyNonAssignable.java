package org.opalj.fpcf.fixtures.benchmark.assignability;

import org.opalj.fpcf.fixtures.benchmark.generals.ClassWithMutableFields;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;

import java.util.*;

public class EffectivelyNonAssignable {

    @EffectivelyNonAssignableField("The field is only assigned once")
    private ClassWithMutableFields tm7 = new ClassWithMutableFields();

    public void foo(){
        tm7.nop();
    }


    @TransitivelyImmutableField("field value has a primitive type and an effectively non assignable field")
    @EffectivelyNonAssignableField("field is not written after initialization")
    private int simpleInitializedFieldWithPrimitiveType = 5;

    @TransitivelyImmutableField(value = "immutable reference and deep immutable type")
    @EffectivelyNonAssignableField(value = "effective immutable field")
    private Integer effectiveImmutableIntegerField = 5;

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField("field is effective immutable")
    private double effectiveImmutableDoubleField = 5d;

    @TransitivelyImmutableField("immutable reference and deep immutable type")
    @EffectivelyNonAssignableField("field is effective immutable")
    private Double effectiveImmutableObjectDoubleField = 5d;

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField(value = "field is not written after initialization")
    private float effectiveImmutableFloatField = 5;

    @TransitivelyImmutableField("field has an immutable field reference and a deep immutable type")
    @EffectivelyNonAssignableField("the field reference is effective immutable")
    private Float effectiveImmutableFloatObjectField = 5f;

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField("field is effective immutable")
    private Byte effectiveImmutableByteObjectField = 5;

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField("field is effective immutable")
    private byte effectiveImmutableByteField = 5;

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField(value = "field is effective immutable")
    private char c = 'a';

    @TransitivelyImmutableField("field value has a primitive type and an immutable field reference")
    @EffectivelyNonAssignableField(value = "field is not written after initialization")
    private long effectiveImmutableLongField = 5;

    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private Long lO = 5l;

    @TransitivelyImmutableField("The concrete assigned object is known to be deep immutable")
    @EffectivelyNonAssignableField("The field is effective immutable")
    private String effectiveImmutableString = "abc";

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("effective immutable reference")
    private List<Object> effectiveImmutableLinkedList = new LinkedList<Object>();

    @TransitivelyImmutableField("The concrete assigned object is known to be deep immutable")
    @EffectivelyNonAssignableField("The field is effective immutable")
    private Object effectiveImmutableObjectReference = new Object();

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("effective immutable reference")
    private Set<Object> effectiveImmutableSet = new HashSet<Object>();

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("effective immutable reference")
    private HashMap<Object, Object> effectiveImmutableHashMap = new HashMap<Object, Object>();

}
