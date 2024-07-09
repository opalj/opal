/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodKeys;
import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * This class offers no factory method.
 * 
 * @author Michael Reif
 * 
 */
@InstantiabilityProperty
class PackageVisibleFactoryMethod {
	
	PackageVisibleFactoryMethod(){
		// do weird stuff
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.IsFactoryMethod)
	private static PackageVisibleFactoryMethod newInstance(){
		return new PackageVisibleFactoryMethod();
	}
}
