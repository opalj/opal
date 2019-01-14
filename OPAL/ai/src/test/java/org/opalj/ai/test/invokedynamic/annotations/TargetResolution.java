/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.test.invokedynamic.annotations;

/**
 * Describes whether the method call instruction is a standard invoke 
 * instruction (invokevirtual,
 * invokestatic, invokespecial, invokeinterface), an invokedynamic, 
 * or a call made through use of
 * the Java reflection API.
 * 
 * @author Arne Lottmann
 */
public enum TargetResolution {
	
    /**
     * Describes a method call made using one of the following instructions:
     * <ul>
     * <li>invokevirtual</li>
     * <li>invokestatic</li>
     * <li>invokespecial</li>
     * <li>invokeinterface</li>
     * </ul>
     * .
     */
    DEFAULT,
    /**
     * Describes a method call based on an invokedynamic instruction.
     */
    DYNAMIC,
    /**
     * Describes a reflective method call.
     */
    REFLECTIVE
}
