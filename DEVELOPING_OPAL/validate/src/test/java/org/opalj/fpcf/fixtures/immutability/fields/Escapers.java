/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.EffectivelyNonAssignableField;
import org.opalj.fpcf.properties.immutability.field_assignability.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

public class Escapers{

}

class TransitiveEscape1 {

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private ClassWithPublicFields tmc = new ClassWithPublicFields();

    public void printTMC(){
        System.out.println(tmc.name);
    }

    public ClassWithPublicFields get(){
        ClassWithPublicFields tmc1 = this.tmc;
        return tmc1;
    }
}

class TransitiveEscape2 {

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private ClassWithPublicFields tmc = new ClassWithPublicFields();

    public void printTMC(){
        System.out.println(tmc.name);
    }

    public ClassWithPublicFields get(){
        ClassWithPublicFields tmc1 = this.tmc;
        ClassWithPublicFields tmc2 = tmc1;
        return tmc2;
    }
}
class OneThatNotEscapesAndOneWithDCL {
    @NonTransitivelyImmutableField(value = "immutable field reference and mutable type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @EffectivelyNonAssignableField(value = "field is only written once",
            analyses = L3FieldAssignabilityAnalysis.class)
    private ClassWithPublicFields tmc1 = new ClassWithPublicFields();

    @MutableField(value = "mutable reference", analyses = {
            L0FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
            L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class
    })
    @LazyInitializedNotThreadSafeFieldReference("")
    private ClassWithPublicFields tmc2;

    public ClassWithPublicFields set() {
        ClassWithPublicFields tmc22 = tmc2;
        if (tmc22 == null) {
            synchronized (this) {
                if (tmc22 == null) {
                    tmc2 = new ClassWithPublicFields();
                }
            }
        }
        return tmc2;
    }
}
class GenericEscapes {

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private SimpleGenericClass sgc;

    public GenericEscapes(ClassWithPublicFields tmc){
        sgc = new SimpleGenericClass(tmc);
    }
}


@NonTransitivelyImmutableClass("")
class GenericEscapesTransitive {
    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private SimpleGenericClass gc1;

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private ClassWithPublicFields tmc;

    public GenericEscapesTransitive(ClassWithPublicFields tmc){
        this.tmc = tmc;
        gc1 = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyNotAbleToResolve{

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private ClassWithPublicFields tmc = new ClassWithPublicFields();

    @NonTransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private SimpleGenericClass sgc;

    public GenericNotEscapesMutualEscapeDependencyNotAbleToResolve() {

        this.sgc = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyAbleToResolve<T>{

    @TransitivelyImmutableField("")
    @EffectivelyNonAssignableField("")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @DependentImmutableField(value = "", analyses = L0FieldImmutabilityAnalysis.class, parameter = "T")
    @EffectivelyNonAssignableField("")
    private SimpleGenericClass<T> sgc;

    public GenericNotEscapesMutualEscapeDependencyAbleToResolve() {
        this.sgc = new SimpleGenericClass(this.fec);
    }
}

@DependentlyImmutableClass("")
final class SimpleGenericClass<T> {
    @DependentImmutableField(value = "", parameter = "T")
    private T t;
    SimpleGenericClass(T t){
        this.t = t;
    }
}
