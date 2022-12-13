/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.openworld.assignability.clone_function;

import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class encompasses different counter examples for the clone pattern.
 */
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
public class ObjectEscapesInCloneFunction {

    @MutableField("Field is assignable")
    @AssignableField("Field can be seen with different values")
    int i;

    @MutableField("Field is assignable")
    @AssignableField("Field is default visible and as a result assignable")
    ObjectEscapesInCloneFunction instance;

    public ObjectEscapesInCloneFunction clone(){
        ObjectEscapesInCloneFunction newInstance = new ObjectEscapesInCloneFunction();
        instance = newInstance;
        newInstance.i = 5;
        return newInstance;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class ThereIsNoNewObjectInCloneFunctionCreated {

    @MutableField("Field is assignable")
    @AssignableField("Field can be assigned multiple times")
    private Integer integer;

    public ThereIsNoNewObjectInCloneFunctionCreated clone(){
        ThereIsNoNewObjectInCloneFunctionCreated instanceCopy = this;
        instanceCopy.integer = new Integer(5);
        return instanceCopy;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapesViaMethod {

    @MutableField("Field is assignable")
    @AssignableField("Field can be seen with different values while calling the clone method.")
    private Object object;

    public EscapesViaMethod clone(){
        EscapesViaMethod newInstance = new EscapesViaMethod();
        Static.setObject(newInstance.object);
        newInstance.object = object;
        return newInstance;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapingThroughIdentityFunction {
    
    @MutableField("Field is assignable")
    @AssignableField("The field value is assigned to the field integerCopy before it is set and, thus, " +
            "it can be seen with multiple values")
    private Integer integer;

    public Integer integerCopy;

    public Integer identity(Integer integer) {
        return integer;
    }

    public EscapingThroughIdentityFunction clone() {
        EscapingThroughIdentityFunction newInstance = new EscapingThroughIdentityFunction();
        this.integerCopy = newInstance.identity(newInstance.integer);
        newInstance.integer = integer;
        return newInstance;
    }
}

class Static {

    public Integer identity(Integer integer){
        return integer;
    }

    public static Integer object;

    public static void setObject(Object object){
        Static.object = Static.object;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class EscapesViaGetterCall {
    
    @MutableField("field is assignable")
    @AssignableField("field can be seen with different values")
    private int i;

    public int getI(){
        return i;
    }

    @MutableField("field is assignable")
    @AssignableField("can be assigned multiple times")
    private int n;

    public EscapesViaGetterCall clone(){
        EscapesViaGetterCall c = new EscapesViaGetterCall();
        this.n = c.getI();
        c.i = this.i;
        return c;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapesViaStaticSetterMethod {

    @MutableField("Field is assignable")
    @AssignableField("Field escapes via static setter")
    private Object object;

    public EscapesViaStaticSetterMethod clone(){
        EscapesViaStaticSetterMethod newInstance = new EscapesViaStaticSetterMethod();
        Object objectCopy = newInstance.object;
        Static.setObject(objectCopy);
        newInstance.object = object;
        return newInstance;
    }
}
