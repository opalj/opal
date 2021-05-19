/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.assignability.clone_function;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
public class ObjectEscapesInCloneFunction {

    //@Immutable
    @MutableField("field is assignable")
    @AssignableField("field can be seen with different values")
    int i;

    //@Immutable
    @MutableField("field is assignable")
    @AssignableField("field can be assigned multiple times")
    ObjectEscapesInCloneFunction instance;

    public ObjectEscapesInCloneFunction clone(){
        ObjectEscapesInCloneFunction c = new ObjectEscapesInCloneFunction();
        instance = c;
        c.i = 5;
        return c;
    }
}



@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class ThereIsNoNewObjectInCloneFunctionCreated {

    @MutableField("field is assignable")
    @AssignableField("field can be assigned multiple times")
    private Object o;

    public ThereIsNoNewObjectInCloneFunctionCreated clone(){
        ThereIsNoNewObjectInCloneFunctionCreated c = this;
        c.o = new Object();
        return c;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class FieldReadInCloneFunction {

    //@Immutable
    @MutableField("")
    @AssignableField("")
    private int i;

    @MutableField("field is assignable")
    @AssignableField("can be assigned multiple times")
    private int n;

    public FieldReadInCloneFunction clone(){
        FieldReadInCloneFunction c = new FieldReadInCloneFunction();
        this.n = c.i;
        c.i = this.i;
        return c;
    }
}

@MutableType("Class is mutable")
@MutableClass("Class has a mutable field")
class GetterCallInCloneFunction {

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

    public GetterCallInCloneFunction clone(){
        GetterCallInCloneFunction c = new GetterCallInCloneFunction();
        this.n = c.getI(); //c.i;
        c.i = this.i;
        return c;
    }
}








