/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.*;
import org.opalj.fpcf.properties.field_mutability.NonFinal;

public class EscapesOfExceptions {

    @NonFinal("the field is global and public and gets modified")
    public static Exception global;

    public static void directThrowException() {
        throw new @EscapeViaAbnormalReturn("the exception is thrown") RuntimeException();
    }

    public static int directCatchedException() {
        try {
            throw new @EscapeInCallee("the exception is catched immediately") RuntimeException();
        } catch (RuntimeException e) {
            return -1;
        }
    }

    public static int multipleExceptionsAllCatched(boolean b) throws Exception {
        Exception e;
        if (b) {
            e = new @EscapeInCallee("the exception is catched") IllegalArgumentException();
        } else {
            e = new @EscapeInCallee("the exception is also catched") IllegalStateException();
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
            e = new @EscapeInCallee("the exception is catched") IllegalArgumentException();
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

    public static void isThrownInConstructor() throws AnException {
        new
                @AtMostNoEscape(value = "analyses do not track the abnormal return any further")
                @EscapeViaAbnormalReturn(value = "the exception is thrown in its constructor", analyses = {})
                        AnException(true);
    }

    static class AnException extends Exception {
        AnException(boolean b) throws AnException {
            if (b) {
                throw this;
            }
        }
    }


    public static void fieldWriteOfException() {
        try {
            thrownExceptionWithField();
        } catch (ExceptionWithField e) {
            e.value = new
                    @NoEscape(value = "the exception is not thrown further", analyses = {})
                    @AtMostNoEscape("we do not track fields")
                            Object();
        }
    }


    public static void thrownExceptionWithField() throws ExceptionWithField {
        if (System.currentTimeMillis() == 123123123)
            throw new @EscapeViaAbnormalReturn("exception is thrown") ExceptionWithField();
    }

    static class ExceptionWithField extends Exception {
        public Object value;
    }
}
