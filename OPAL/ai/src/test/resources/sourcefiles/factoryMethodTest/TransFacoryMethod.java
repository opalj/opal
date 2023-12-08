/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityKeys;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * @author Michael Reif
 * 
 */
@InstantiabilityProperty(InstantiabilityKeys.NotInstantiable)
public class TransFacoryMethod {
	
	private TransFacoryMethod(){
		//this class can not be instantiated.
	}
	
	@FactoryMethodProperty
	protected static PackageVisibleFactoryMethod newInstance(){
		return new PackageVisibleFactoryMethod();
	}
}
