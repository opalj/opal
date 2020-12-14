/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.type;

import org.opalj.br.fpcf.analyses.L0ClassImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.br.fpcf.analyses.L0TypeImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.classes.DeepImmutableClass;
import org.opalj.fpcf.properties.immutability.classes.MutableClass;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.ImmutableFieldReference;
import org.opalj.fpcf.properties.immutability.references.MutableFieldReference;
import org.opalj.fpcf.properties.immutability.types.DeepImmutableType;
import org.opalj.fpcf.properties.immutability.types.MutableType;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1ClassImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L1TypeImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

@DeepImmutableType("has only deep immutable fields and is final")
@DeepImmutableClass("has only deep immutable fields")
public final class WithMutableAndImmutableFieldType {

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DeepImmutableField(value = "immutable reference and deep immutable type",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "can not handle deep immutability", analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class} )
    @ImmutableFieldReference("private, effective immutable field")
    private FinalEmptyClass fec = new FinalEmptyClass();

    @MutableField(value="can not handle effective immutability", analyses = L0FieldImmutabilityAnalysis.class)
    @DeepImmutableField(value = "assigned object is known and can not escape",
            analyses = L3FieldImmutabilityAnalysis.class)
    @ShallowImmutableField(value = "has mutable type but is effective final",
            analyses = {L1FieldImmutabilityAnalysis.class, L2FieldImmutabilityAnalysis.class})
    @ImmutableFieldReference(value = "private, effectively final field",
            analyses = L0FieldReferenceImmutabilityAnalysis.class)
    private SimpleMutableClass tmc = new SimpleMutableClass();
}

@MutableType("class is deep immutable but extensible")
@DeepImmutableClass("has no fields")
class EmptyClass {
}

@DeepImmutableClass("Class has no fields")
@DeepImmutableType("Class has no fields and is final")
final class FinalEmptyClass {
}

@MutableType(value = "has a public instance field",
        analyses = {L0TypeImmutabilityAnalysis.class, L1TypeImmutabilityAnalysis.class})
@MutableClass(value = "has a public instance field",
        analyses = {L0ClassImmutabilityAnalysis.class, L1ClassImmutabilityAnalysis.class})
class SimpleMutableClass{

    @MutableField(value = "field is public",
            analyses = {L0FieldReferenceImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class,
                    L2FieldImmutabilityAnalysis.class, L3FieldImmutabilityAnalysis.class})
    @MutableFieldReference(value = "field is public", analyses = L0FieldReferenceImmutabilityAnalysis.class)
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
