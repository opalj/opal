/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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
package org.opalj.fpcf.fixtures.field_locality;

import org.opalj.fpcf.properties.field_locality.LocalFieldWithGetter;
import org.opalj.fpcf.properties.field_locality.NoLocalField;

public class NonLocalFields implements Cloneable {

    @NoLocalField("Not local as it is assigned by constructor")
    private Object assigned;

    @LocalFieldWithGetter("Not local as it escapes getValue")
    private Object escapes;

    @NoLocalField("Not local as cloned object may escape without this being overwritten")
    private Object copyEscapes;

    public NonLocalFields(Object input){
        assigned = input;
    }

    public Object getValue(){
        return escapes;
    }

    public Object clone() throws CloneNotSupportedException {
        NonLocalFields copy = (NonLocalFields) super.clone();
        copy.assigned = new Object();
        copy.escapes = new Object();
        if(System.nanoTime() > 1000){
            copy.copyEscapes = new Object();
        }else{
            copyEscapes = new Object(); // Does not assign field of the copy, but of the original!
        }
        return copy;
    }
}
