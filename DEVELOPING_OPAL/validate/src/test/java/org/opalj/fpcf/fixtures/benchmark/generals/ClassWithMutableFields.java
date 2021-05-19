/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.generals;

//import edu.cmu.cs.glacier.qual.Immutable;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.fpcf.properties.immutability.types.MutableType;

@MutableType("The class has mutable fields")
@MutableClass("The class has mutable fields")
public class ClassWithMutableFields {

    //@Immutable
    @MutableField(value = "field is public")
    @AssignableField(value = "field is public")
    public String name = "name";

    //@Immutable
    @MutableField("the field has a mutable field reference")
    @AssignableField("the field is protected")
    protected FinalClassWithNoFields fec1 = new FinalClassWithNoFields();

    //@Immutable
    @MutableField("Because of Mutable Reference")
    @AssignableField("Because it is declared as protected")
    protected ClassWithNonTransitivelyImmutableFields cwpf1 =
            new ClassWithNonTransitivelyImmutableFields(new Object(), new ClassWithMutableFields(), new ClassWithMutableFields());

    //@Immutable
    @MutableField("This field is not final and public and thus assignable")
    @AssignableField("This field is not final and public")
    public int publicN = 5;

    //@Immutable
    @MutableField("This field is not final and protected and thus assignable")
    @AssignableField("This field is not final and protected")
    protected  int protectedN = 5;

    //@Immutable
    @MutableField("This field is not final and default-visible and thus assignable")
    @AssignableField("This field is not final and default-visible")
    int defaultVisibleN = 5;

    //@Immutable
    @MutableField("The field can be set and is thus assignable")
    @AssignableField("The field can be set")
    private int n = 5;

    public void setN(){
        this.n = 5;
    }

    //@Immutable
    @MutableField("The field can be incremented and is thus assignable")
    @AssignableField("The field can be incremented")
    private int iCompound;

    //@Immutable
    @MutableField("The field can be set in the constructor of another instance and is thus assignable")
    @AssignableField("The field can be set in the constructor of another instance")
    private int iConstr;

    public void setN(int n){
        this.n = n;
    }

    public static void updateI(ClassWithMutableFields s) {
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
    @MutableField("")
    @AssignableField("")
    private static int manualIncrementingCounter;

    //@Immutable
    @MutableField("")
    @AssignableField("")
    private static int manualCounter;

    //@Immutable
    @MutableField("")
    @AssignableField("")
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