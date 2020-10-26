/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

//import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;

import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

public class Template {

    //@LazyInitializedNotThreadSafeReferenceAnnotation("")
    @MutableField(value = "mutable field reference", analyses = L3FieldImmutabilityAnalysis.class)
    @MutableFieldReference(value = "can not handle this kind of lazy initialization",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private Template _template;


    @ShallowImmutableField(value = "immutable reference and mutable type",
            analyses = L3FieldImmutabilityAnalysis.class)
    private Template _parent;

    public Template(Template parent) {
        _parent = parent;
    }

    protected final Template getParent() {
        return _parent;
    }

    protected Template getTemplate() {

        if (_template == null) {
            Template parent = this;
            while (parent != null)
                parent = parent.getParent();
            _template = parent;
        }
        return _template;
    }

}
