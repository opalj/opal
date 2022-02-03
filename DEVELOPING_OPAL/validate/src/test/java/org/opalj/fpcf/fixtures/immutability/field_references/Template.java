/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references;

//import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.field_assignability.AssignableField;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldassignability.L3FieldAssignabilityAnalysis;

public class Template {

    //@LazyInitializedNotThreadSafeReferenceAnnotation("")
    @MutableField(value = "mutable field reference", analyses = L0FieldImmutabilityAnalysis.class)
    @AssignableField(value = "can not handle this kind of lazy initialization",
            analyses = L3FieldAssignabilityAnalysis.class)
    private Template _template;


    @NonTransitivelyImmutableField(value = "immutable reference and mutable type",
            analyses = L0FieldImmutabilityAnalysis.class)
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