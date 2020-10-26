/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.br.fpcf.analyses.L0FieldImmutabilityAnalysis;
import org.opalj.fpcf.properties.immutability.fields.DeepImmutableField;
import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.fields.ShallowImmutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeButDeterministicReference;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.tac.fpcf.analyses.L1FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.L2FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.L3FieldImmutabilityAnalysis;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L0FieldReferenceImmutabilityAnalysis;

/**
 * This class represents lazy initialization in the simplest way
 * with one guard and no synchronization
 *
 * @author Tobias Roth
 *
 */
public class simpleLazyInitialization {
	@LazyInitializedNotThreadSafeFieldReference(value = "the write to the object reference simpleLazyInitialization" +
			" is not atomic",
			analyses = L0FieldReferenceImmutabilityAnalysis.class)
	private static simpleLazyInitialization simpleLazyInitialization;
	
	public static simpleLazyInitialization init() {
		if(simpleLazyInitialization ==null)
			simpleLazyInitialization = new simpleLazyInitialization();
		return simpleLazyInitialization;
	}

	@ShallowImmutableField(value = "can not handle transitive immutability",
			analyses = L2FieldImmutabilityAnalysis.class)
	@DeepImmutableField(value = "field has immutable field reference an primitive type",
			analyses = L3FieldImmutabilityAnalysis.class)
	@MutableField(value = "can not handle effective immutability and lazy initialization",
			analyses = {L0FieldImmutabilityAnalysis.class, L1FieldImmutabilityAnalysis.class})
	@LazyInitializedNotThreadSafeButDeterministicReference(value = "deterministic write due to guarded primitive type",
			analyses = {L0FieldReferenceImmutabilityAnalysis.class})
	private int i = 0;
	public int hashcode() {
		if(i==0)
			i = 5;
		return i;
	}
}


