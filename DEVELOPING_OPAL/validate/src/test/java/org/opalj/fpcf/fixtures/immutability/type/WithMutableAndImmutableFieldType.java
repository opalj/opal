/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldAssignabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.TransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.classes.NonTransitivelyImmutableClass;
import org.opalj.fpcf.properties.immutability.fields.TransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.fpcf.properties.immutability.references.AssignableFieldReference;
import org.opalj.fpcf.properties.immutability.types.TransitivelyImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.fpcf.properties.immutability.types.NonTransitiveImmutableType;
import org.opalj.tac.fpcf.analyses.L1FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldAssignabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

@NonTransitiveImmutableType("has only deep immutable fields and is final")
@NonTransitivelyImmutableClass("has only deep immutable fields")
public final class WithMutableAndImmutableFieldType {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @TransitivelyImmutableField(value = "immutable reference and deep immutable type",
            analyses = L0FieldImmutabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "can not handle deep immutability", analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class} )
    @NonAssignableFieldReference("private, effective immutable field")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @MutableField(value="can not handle effective immutability", analyses = L0FieldAssignabilityAnalysis.class)
    @NonTransitivelyImmutableField(value = "has mutable type but is effectively final",
            analyses = {L1FieldAssignabilityAnalysis.class, L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @NonAssignableFieldReference(value = "private, effectively final field",
            analyses = L3FieldAssignabilityAnalysis.class)
    private SimpleMutableClass tmc = new SimpleMutableClass();
}

@MutableType("class is deep immutable but extensible")
@TransitivelyImmutableClass("has no fields")
class EmptyClass {
}

@TransitivelyImmutableClass("Class has no fields")
@TransitivelyImmutableType("Class has no fields and is final")
final class FinalEmptyClass {
}

@MutableType(value = "has a public instance field",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "has a public instance field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class SimpleMutableClass{

    @MutableField(value = "field is public",
            analyses = {L3FieldAssignabilityAnalysis.class, L1FieldAssignabilityAnalysis.class,
                    L2FieldAssignabilityAnalysis.class, L0FieldImmutabilityAnalysis.class})
    @AssignableFieldReference(value = "field is public", analyses = L3FieldAssignabilityAnalysis.class)
    public int n=0;
    //com.sun.corba.se.impl.io.ObjectStreamClass.noArgsList
    //sun.misc.UCDecoder.tmp
    //com.sun.xml.internal.org.jvnet.mimepull.DataHead.consumedAt
    //com.sun.xml.internal.bind.DatatypeConverterImpl.hexCode
    //com.sun.xml.internal.ws.client.sei.BodyBuilder $Bare.methodPos
    //com.sun.xml.internal.ws.client.sei.ValueSetter $Param.idx
    //com.sun.xml.internal.ws.client.sei.CallbackMethodHandler
    //jdk.internal.dynalink.beans.MaximallySpecific.DYNAMIC_METHOD_TYPE_GETTER
    //java.time.format.DateTimeFormatterBuilder.FIELD_MAP
    //java.time.format.DateTimeFormatter.resolverFields
    //com.sun.crypto.provider.DESCrypt.initPermLeft0
    //com.sun.beans.editors.FontEditor.styles
    //java.time.format.DateTimeFormatter.resolverFields
}
