/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package annotations.property;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An entity along with a specific property value it should have.
 *
 * @author Dominik Helm
 */
@Retention(RUNTIME)
public @interface EP {

    /**
     * The entity that should have a specific property value.
     *
     * Supported entities are:
     * <ul>
     *  <li>Classes are specified using the fully qualified name, e.g.
     *      <code>java/lang/Math</code></li>
     *  <li>Fields are specified by &lt;className&gt;.&lt;fieldName&gt;, e.g.
     *      <code>java/lang/Long.value</code></li>
     *  <li>Methods are specified by &lt;className&gt;.&lt;methodName&gt;&lt;methodDescriptor&gt;,
     *      e.g. <code>java/lang/Math.abs(I)I</code>
     * </ul>
     */
    String e();

    /**
     * The name of the property key of the required property.
     *
     * By convention, it is the simple name of the property class, e.g. <code>pk="Purity"</code>.
     * The actual mapping to the property key is up to the test, though.
     */
    String pk() default "";

    /**
     * The expected entity's property value; should be left empty if the entity should not have
     * the respective property.
     *
     * <i>Implementation note: This can't be a specific enum as it must be able to hold any
     * property.</i>
     */
    String p() default "";
}
