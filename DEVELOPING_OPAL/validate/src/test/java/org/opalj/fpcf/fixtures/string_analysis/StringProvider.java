/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.string_analysis;

public class StringProvider {

    /**
     * Returns "[packageName].[className]".
     */
    public static String getFQClassName(String packageName, String className) {
        return packageName + "." + className;
    }

}
