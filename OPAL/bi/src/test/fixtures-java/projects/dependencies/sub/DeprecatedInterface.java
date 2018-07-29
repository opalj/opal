/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package dependencies.sub;

import dependencies.TestInterface;

/**
 * @author Thomas Schlosser
 * 
 */
@Deprecated
public interface DeprecatedInterface extends TestInterface, MarkerInterface {

    @Deprecated
    public void deprecatedMethod();

    public void methodDeprParam(@Deprecated int i);
}
