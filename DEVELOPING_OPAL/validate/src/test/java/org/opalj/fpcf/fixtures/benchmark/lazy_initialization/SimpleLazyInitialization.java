/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.benchmark.lazy_initialization;

import org.opalj.fpcf.properties.immutability.references.LazyInitializedNotThreadSafeFieldReference;

/**
 * This class represents lazy initialization in the simplest way
 * with one guard and no synchronization
 *
 * @author Tobias Roth
 *
 */
public class SimpleLazyInitialization {

	@LazyInitializedNotThreadSafeFieldReference("the write to the object reference simpleLazyInitialization is not atomic")
	private static SimpleLazyInitialization simpleLazyInitialization;
	
	public static SimpleLazyInitialization init() {
		if(simpleLazyInitialization ==null)
			simpleLazyInitialization = new SimpleLazyInitialization();
		return simpleLazyInitialization;
	}

	@LazyInitializedNotThreadSafeFieldReference("deterministic write due to guarded primitive type")
	private int i = 0;
	public int hashcode() {
		if(i==0)
			i = 5;
		return i;
	}
}


