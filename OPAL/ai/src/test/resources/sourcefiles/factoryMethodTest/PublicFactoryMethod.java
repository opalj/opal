/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.FactoryMethodKeys;
import org.opalj.fpcf.test.annotations.FactoryMethodProperty;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * This class offers a fake and a real factory method.
 * 
 * @author Michael Reif
 *
 */
@InstantiabilityProperty
public class PublicFactoryMethod {

	private PublicFactoryMethod(){
		// I'm private
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.NotFactoryMethod)
	public static void newInstanceAfterOtherConstructorCall(){
		new NoFactoryButInstantiable();
	}
	
	@FactoryMethodProperty(FactoryMethodKeys.IsFactoryMethod)
	public static PublicFactoryMethod newInstance(){
		return new PublicFactoryMethod();
	}
}
