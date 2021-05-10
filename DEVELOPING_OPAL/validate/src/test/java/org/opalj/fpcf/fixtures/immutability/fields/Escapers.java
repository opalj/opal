/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.fields;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DependentlyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.DependentImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

public class Escapers{

}

class TransitiveEscape1 {

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
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
    @NonAssignableFieldReference("")
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
            analyses = L3FieldImmutabilityAnalysis.class)
    @NonAssignableFieldReference(value = "field is only written once",
            analyses = L3FieldAssignabilityAnalysis.class)
    private ClassWithPublicFields tmc1 = new ClassWithPublicFields();

    @MutableField(value = "mutable reference", analyses = {
            L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
            L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class
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
    @NonAssignableFieldReference("")
    private SimpleGenericClass sgc;

    public GenericEscapes(ClassWithPublicFields tmc){
        sgc = new SimpleGenericClass(tmc);
    }
}


@NonTransitivelyImmutableClass("")
class GenericEscapesTransitive {
    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private SimpleGenericClass gc1;

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private ClassWithPublicFields tmc;

    public GenericEscapesTransitive(ClassWithPublicFields tmc){
        this.tmc = tmc;
        gc1 = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyNotAbleToResolve{

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private ClassWithPublicFields tmc = new ClassWithPublicFields();

    @NonTransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private SimpleGenericClass sgc;

    public GenericNotEscapesMutualEscapeDependencyNotAbleToResolve() {

        this.sgc = new SimpleGenericClass(this.tmc);
    }
}

class GenericNotEscapesMutualEscapeDependencyAbleToResolve<T>{

    @TransitivelyImmutableField("")
    @NonAssignableFieldReference("")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @DependentImmutableField(value = "", analyses = L3FieldImmutabilityAnalysis.class, parameter = "T")
    @NonAssignableFieldReference("")
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
