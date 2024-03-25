/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.xl;


/**
 * to be implemented once TAJS pts have been implemented.
 * Intermediate implementation specifies location of ScriptEngine.eval() call, and allocated type.
 * source file (specified through class) and line number are used
 */
public @interface JSEnvironmentBinding {
    String identifier();
    String value();
}