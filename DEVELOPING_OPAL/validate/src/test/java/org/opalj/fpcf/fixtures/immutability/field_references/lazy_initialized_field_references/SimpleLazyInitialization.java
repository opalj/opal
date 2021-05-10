/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.immutability.field_references.lazy_initialized_field_references;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;
import org.opalj.tac.fpcf.analyses.immutability.fieldreference.L3FieldAssignabilityAnalysis;

/**
 * This class represents lazy initialization in the simplest way
 * with one guard and no synchronization
 *
 * @author Tobias Roth
 *
 */
public class SimpleLazyInitialization {
	@LazyInitializedNotThreadSafeFieldReference(value = "the write to the object reference simpleLazyInitialization" +
			" is not atomic",
			analyses = L3FieldAssignabilityAnalysis.class)
	private static SimpleLazyInitialization simpleLazyInitialization;
	
	public static SimpleLazyInitialization init() {
		if(simpleLazyInitialization ==null)
			simpleLazyInitialization = new SimpleLazyInitialization();
		return simpleLazyInitialization;
	}

	/*@ShallowImmutableField(value = "can not handle transitive immutability",
			analyses = L2FieldImmutabilityAnalysis.class)
	@DeepImmutableField(value = "field has immutable field reference an primitive type",
			analyses = L3FieldImmutabilityAnalysis.class) */
	@MutableField(value = "can not handle effective immutability and lazy initialization")
	@LazyInitializedNotThreadSafeFieldReference(value = "deterministic write due to guarded primitive type",
			analyses = {L3FieldAssignabilityAnalysis.class})
	private int i = 0;
	public int hashcode() {
		if(i==0)
			i = 5;
		return i;
	}
}


