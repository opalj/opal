/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package factoryMethodTest;

import org.opalj.fpcf.test.annotations.InstantiabilityKeys;
import org.opalj.fpcf.test.annotations.InstantiabilityProperty;

/**
 * 
 * @author Michael Reif
 *
 */
@InstantiabilityProperty(InstantiabilityKeys.NotInstantiable)
abstract class PackagePrivateAbstractClass {

}
