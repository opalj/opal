package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.NotThreadSafeLazyInitializationAnnotation;

public class SimpleLazyInstantiation{
	@NotThreadSafeLazyInitializationAnnotation("")
	private static SimpleLazyInstantiation instance;
	
	public static SimpleLazyInstantiation init() {
		if(instance==null)
			instance = new SimpleLazyInstantiation();
		return instance;
	}
}
