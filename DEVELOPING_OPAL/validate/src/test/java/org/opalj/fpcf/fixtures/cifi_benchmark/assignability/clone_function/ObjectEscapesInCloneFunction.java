/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.assignability.clone_function;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.fixtures.cifi_benchmark.common.CustomObject;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

/**
 * This class encompasses different counter examples for the clone pattern.
 */
//@Immutable
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
public class ObjectEscapesInCloneFunction {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field can be seen with different values")
    int i;

    //@Immutable
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

//@Immutable
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapesViaMethod {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field can be seen with different values while calling the clone method.")
    private CustomObject customObject;

    public EscapesViaMethod clone(){
        EscapesViaMethod newInstance = new EscapesViaMethod();
        Static.setCustomObject(newInstance.customObject);
        newInstance.customObject = customObject;
        return newInstance;
    }
}

//@Immutable
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapingThroughIdentityFunction {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("The field value is assigned t the field integerCopy before it is set and, thus, " +
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

    public static Integer customObject;

    public static void setCustomObject(CustomObject customObject){
        Static.customObject = Static.customObject;
    }

}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class EscapesViaGetterCall {

    //@Immutable
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
        this.n = c.getI(); //c.i;
        c.i = this.i;
        return c;
    }
}

//@Immutable
@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
final class EscapesViaStaticSetterMethod {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field escapes via static setter")
    private CustomObject customObject;

    public EscapesViaStaticSetterMethod clone(){
        EscapesViaStaticSetterMethod newInstance = new EscapesViaStaticSetterMethod();
        CustomObject customObjectCopy = newInstance.customObject;
        Static.setCustomObject(customObjectCopy);
        newInstance.customObject = customObject;
        return newInstance;
    }
}
