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
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.EscapeViaAbnormalReturn;
import org.opalj.fpcf.properties.escape.EscapeViaStaticField;
import org.opalj.fpcf.properties.escape.MaybeNoEscape;
import org.opalj.fpcf.properties.escape.NoEscape;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

public class EscapesOfExceptions {

    @NonFinal("the field is global and public and gets modified")
    public static Exception global;

    public static void directThrowException() {
        throw new @EscapeViaAbnormalReturn("the exception is thrown") RuntimeException();
    }

    public static int directCatchedException() {
        try {
            throw new @NoEscape("the exception is catched immediately") RuntimeException();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static int multipleExceptionsAllCatched(boolean b) throws Exception {
        Exception e;
        if (b) {
            e = new @NoEscape("the exception is catched") IllegalArgumentException();
        } else {
            e = new @NoEscape("the exception is also catched") IllegalStateException();
        }
        try {
            throw e;
        } catch (IllegalArgumentException e1) {
            return -1;
        } catch (IllegalStateException e1) {
            return -2;
        }
    }

    public static int multipleExceptionsSomeCatched(boolean b) throws Exception {
        Exception e = null;
        if (b) {
            e = new @NoEscape("the exception is catched") IllegalArgumentException();
        } else {
            e = new @EscapeViaAbnormalReturn(
                    "the exception is not catched") IllegalStateException();
        }
        try {
            throw e;
        } catch (IllegalArgumentException e1) {
            return -1;
        }
    }

    public static void thrownInCatchBlock() {
        try {
            throw new @EscapeViaAbnormalReturn(
                    "the exception is thrown again in catch-block") RuntimeException();
        } catch (Exception e) {
            throw e;
        }
    }

    public static void escapesGloballyInCatchBlock() {
        try {
            throw new @EscapeViaStaticField(
                    "the exception escapes in catch-block") RuntimeException();
        } catch (Exception e) {
            global = e;
        }
    }

    public static void isThrownInConstructor() throws AnException{
        new
                @MaybeNoEscape(value = "analyses do not track the abnormal return any further")
                @EscapeViaAbnormalReturn(value = "the exception is thrown in its constructor", analyses = {})
                        AnException(true);
    }

    static class AnException extends Exception {
        AnException(boolean b) throws AnException{
            if (b) {
                throw this;
            }
        }
    }
}
