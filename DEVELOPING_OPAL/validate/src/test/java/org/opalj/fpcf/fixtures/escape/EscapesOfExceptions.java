package org.opalj.fpcf.fixtures.escape;

import org.opalj.fpcf.properties.escape.EscapeViaAbnormalReturn;
import org.opalj.fpcf.properties.escape.EscapeViaStaticField;
import org.opalj.fpcf.properties.escape.NoEscape;

public class EscapesOfExceptions {

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
        Exception e = null;
        if (b) {
            e = new @NoEscape("the exception is catched") IllegalArgumentException();
        } else {
            e = new @NoEscape("the exception is catched") IllegalStateException();
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
                    "the exception is thrown in catch-block") RuntimeException();
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
}
