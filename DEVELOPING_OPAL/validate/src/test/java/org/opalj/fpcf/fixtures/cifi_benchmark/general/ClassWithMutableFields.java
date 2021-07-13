/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.cifi_benchmark.general;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("The class has mutable fields")
@MutableClass("The class has mutable fields")
public class ClassWithMutableFields {

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("Field is public and not final and as a result assignable")
    public String publicNonFinalField = "name";

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("The field is protected and not final and as a result assignable")
    protected FinalClassWithNoFields protectedNonFinalField = new FinalClassWithNoFields();

    //@Immutable
    @MutableField("Field is assignable")
    @AssignableField("The field is not final and default-visible and as a result assignable")
    int defaultVisibleNonFinalField = 5;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field can be written multiple times due to its setter")
    private int privateNonFinalFieldWithSetter = 5;

    public void setPrivateNonFinalFieldWithSetter(int n){
        this.privateNonFinalFieldWithSetter = n;
    }

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field can be incremented and is thus assignable")
    private int iCompound;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field can be set in the constructor of another instance and is thus assignable")
    private int iConstr;

    public static void incrementI(ClassWithMutableFields s) {
        if (s != null) {
            s.iCompound += 1;
        }
    }

    public static void setI(ClassWithMutableFields s, int n){
        if(s!=null){
            s.iConstr = n;
        }
    }

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field can be written multiple times")
    private static int manualIncrementingCounter;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The field can be written multiple times")
    private static int manualCounter;

    //@Immutable
    @MutableField("The field is assignable")
    @AssignableField("The static field is written everytime a new instance is created")
    private static int instanceCounter;

    public ClassWithMutableFields() {
        instanceCounter = instanceCounter + 1;
    }

    public void incrementCounter() {
        manualIncrementingCounter = manualIncrementingCounter + 1;
    }

    public void setCounter(int n){
        manualCounter = n;
    }

    public void nop(){}

}