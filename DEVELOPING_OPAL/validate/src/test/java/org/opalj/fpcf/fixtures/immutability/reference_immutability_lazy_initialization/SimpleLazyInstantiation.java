package org.opalj.fpcf.fixtures.immutability.reference_immutability_lazy_initialization;

import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeButDeterministicReferenceAnnotation;
import org.opalj.fpcf.properties.reference_immutability.LazyInitializedNotThreadSafeReferenceAnnotation;

public class SimpleLazyInstantiation{
	@LazyInitializedNotThreadSafeReferenceAnnotation("")
	private static SimpleLazyInstantiation instance;
	
	public static SimpleLazyInstantiation init() {
		if(instance==null)
			instance = new SimpleLazyInstantiation();
		return instance;
	}
}

class SimpleLazyIntInstantiation{
	@LazyInitializedNotThreadSafeButDeterministicReferenceAnnotation("")
	private int i = 0;
	public int hashcode() {
		if(i==0)
			i = 5;
		return i;
	}
}

class SimpleLazyObjectsInstantiation{
	@LazyInitializedNotThreadSafeReferenceAnnotation("")
	private static SimpleLazyObjectsInstantiation instance;
	public static SimpleLazyObjectsInstantiation getInstance() {
		if(instance==null)
			instance = new SimpleLazyObjectsInstantiation();
		return instance;
	}
}


