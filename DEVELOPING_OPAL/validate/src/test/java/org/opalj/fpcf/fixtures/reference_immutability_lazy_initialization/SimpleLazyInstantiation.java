package org.opalj.fpcf.fixtures.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability_lazy_initialization.NotThreadSafeLazyInitializationAnnotation;

public class SimpleLazyInstantiation{
	@NotThreadSafeLazyInitializationAnnotation("")
	private SimpleLazyInstantiation instance;
	
	public SimpleLazyInstantiation init() {
		SimpleLazyInstantiation result;
		result = instance == null ? new SimpleLazyInstantiation() : instance;
		return result;
	}
}
