/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodKeys;
import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * This class has a factory method. Native methods have to be considered as factories,
 * because we have to assume, that the native part creates an instance of this class.
 * 
 * @author Michael Reif
 *
 */
@InstantiabilityProperty
public class NativeFactoryMethod {

	private NativeFactoryMethod(){
		
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.IsFactoryMethod)
	public static native void newInstance();
}
