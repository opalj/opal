/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.InstantiabilityKeys;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * This class is used for test purpose only.
 *
 * @author Michael Reif
 */
@InstantiabilityProperty(InstantiabilityKeys.NotInstantiable)
public class OnlyPrivateConstructors {

	private OnlyPrivateConstructors(){

	}

	private OnlyPrivateConstructors(Object object){

	}
}
