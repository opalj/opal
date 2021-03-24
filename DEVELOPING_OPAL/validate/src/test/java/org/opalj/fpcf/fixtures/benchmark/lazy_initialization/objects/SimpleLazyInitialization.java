package org.opalj.fpcf.fixtures.benchmark.lazy_initialization.objects;

import org.opalj.fpcf.properties.immutability.fields.MutableField;
import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;

class SimpleLazyInitialization {

	@LazyInitializedNotThreadSafeFieldReference("deterministic write due to guarded primitive type")
	private int i = 0;
	public int hashcode() {
		if(i==0)
			i = 5;
		return i;
	}

	@MutableField("")
	@LazyInitializedNotThreadSafeFieldReference("the write to the object reference simpleLazyInitialization is not atomic")
	private static Object simpleLazyInitialization;

	public static Object init() {
		if (simpleLazyInitialization == null)
			simpleLazyInitialization = new Object();
		return simpleLazyInitialization;
	}

}