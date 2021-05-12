package org.opalj.fpcf.fixtures.immutability.sandbox31;

import org.opalj.fpcf.properties.immutability.fields.NonTransitivelyImmutableField;
import org.opalj.fpcf.properties.immutability.references.NonAssignableFieldReference;
import org.opalj.tac.fpcf.analyses.immutability.L0FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

class TwoVirgin<A, B, C> {

    @NonTransitivelyImmutableField(value="field has generic parameter", analyses = L0FieldImmutabilityAnalysis.class)
    @NonAssignableFieldReference(value = "field is effective immutable",
            analyses = L3FieldAssignabilityAnalysis.class)
    private GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C> gc1;

    public TwoVirgin(A a, B b, C c, GenericBaseClass<GenericBaseClass, A, A> gc1) {
        this.gc1 = new GenericBaseClass<GenericBaseClass<GenericBaseClass, A, A>, B, C>(gc1,b,c);
    }
}

final class GenericBaseClass<T1,T2,T3> {

    private T1 t1;

    private T2 t2;

    private T3 t3;

    public GenericBaseClass(T1 t1, T2 t2, T3 t3){
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
    }

   ///// com.sun.rowset.JoinRowSetImpl.vecJoinType
    ///// com.sun.xml.internal.ws.message.DOMHeader.node
    /////java.time.chrono.ChronoLocalDateTimeImpl.date
    ///// java.time.chrono.ChronoZonedDateTimeImpl.dateTime
   // com.sun.xml.internal.ws.policy.sourcemodel.PolicyModelTranslator $RawAlternative.allNestedPolicies
    //com.sun.xml.internal.ws.policy.sourcemodel.PolicyModelTranslator $RawPolicy.alternatives
    //sun.print.PrintServiceLookupProvider.aix_defaultPrinterEnumeration
    //-com.sun.xml.internal.ws.client.sei.ValueSetter $Param
    //java.util.ResourceBundle.keySet
}
