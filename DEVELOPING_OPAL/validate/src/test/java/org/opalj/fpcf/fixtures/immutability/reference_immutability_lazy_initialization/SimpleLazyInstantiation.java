package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation;

public class SimpleLazyInstantiation{
	@LazyInitializedNotThreadSafeOrNotDeterministicReferenceAnnotation("")
	private static SimpleLazyInstantiation instance;
	
	public static SimpleLazyInstantiation init() {
		if(instance==null)
			instance = new SimpleLazyInstantiation();
		return instance;
	}
}
