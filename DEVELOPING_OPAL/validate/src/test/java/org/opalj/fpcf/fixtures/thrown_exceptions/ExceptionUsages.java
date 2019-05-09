/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.thrown_exceptions;

import org.opalj.br.fpcf.analyses.L1ThrownExceptionsAnalysis;
import org.opalj.fpcf.properties.thrown_exceptions.DoesNotThrowException;
import org.opalj.fpcf.properties.thrown_exceptions.ExpectedExceptions;
import org.opalj.fpcf.properties.thrown_exceptions.ExpectedExceptionsByOverridingMethods;
import org.opalj.fpcf.properties.thrown_exceptions.Types;

/**
 * Test methods for the thrown exceptions analysis.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
public class ExceptionUsages {

    //
    // CASES RELATED TO NO EXCEPTIONS
    //

    @DoesNotThrowException(reason="just returns constant", requires={})
    public static int staticDoesNotThrowException() {
        return 1;
    }

    @DoesNotThrowException(
            reason="infinite self-recursive call",
            requires = { L1ThrownExceptionsAnalysis.class}
    )
    public static int staticCallDoesNotThrowException() {
        staticCallDoesNotThrowException();
        return 1337;
    }

    @DoesNotThrowException(
            reason="just returns constant",
            requires={}
    )
    public int doesNotThrowException() {
        return 2;
    }

    @DoesNotThrowException(
            reason="just returns constant, is final, may not be overridden",
            requires={}
    )
    public final int finalDoesNotThrowException() {
        return 2;
    }

    @ExpectedExceptionsByOverridingMethods(
            reason = "callee does not throw exception, but method may be overridden"
    )
    public int callDoesNotThrowException() {
        return doesNotThrowException();
    }

    @ExpectedExceptionsByOverridingMethods(
            reason = "self-recursive methods call (StackOverflows are generally ignored by OPAL)," +
                    " may be overridden"
    )
    public int selfRecursiveMethod(boolean b) {
        if (b) {
            return selfRecursiveMethod(false);
        } else {
            return 42;
        }
    }

    @ExpectedExceptionsByOverridingMethods(
            reason = "mutual recursive method calls which throw no exception, call may be " +
                    "overridden"
    )
    public int cycleA(boolean b) {
        if (b) {
            return cycleB();
        }
        return 42;
    }

    @ExpectedExceptionsByOverridingMethods(
            reason = "mutual recursive method calls which throw no exception, call may be " +
                    "overridden"
    )
    public int cycleB() {
        cycleA(false);
        return 42;
    }

    //
    // CASES RELATED TO EXCEPTIONS
    //

    @ExpectedExceptions()
    public static int staticThrowsException() {
        throw new NullPointerException();
    }

    @ExpectedExceptions(
            @Types(concrete = {ArithmeticException.class})
    )
    public static int divByZero() {
        return 2/0;
    }

    @ExpectedExceptions()
    public static int staticCallThrowsException() {
        staticThrowsException();
        return 42;
    }

    @ExpectedExceptions()
    public static final int staticFinalThrowsException() {
        throw new NullPointerException();
    }

    @ExpectedExceptions()
    public static final int staticFinalCallThrowsException() {
        staticThrowsException();
        return 42;
    }

    @ExpectedExceptions()
    public int throwExceptionFromParameter(RuntimeException re) {
        re.printStackTrace();
        throw re;
    }

    @ExpectedExceptions()
    public int throwException() {
        throw new NullPointerException();
    }

    @ExpectedExceptionsByOverridingMethods(
            reason="method call, may be overridden by unknown class"
    )
    public int callThrowException() {
        return throwException();
    }

    @ExpectedExceptions()
    public int simpleCycle(boolean b) {
        if (b) {
            simpleCycle(false);
        } else {
            throw new NullPointerException();
        }
        return 42;
    }

    private static class Foo {
        @ExpectedExceptions()
        public int baz() {
            throw new NullPointerException();
        }

        @DoesNotThrowException(
            reason="just returns constant, is not final, class is private, may not be overridden",
            requires={}
        )
        public int qux() {
            return 42;
        }

        @ExpectedExceptions()
        final public int finalBaz() {
            throw new NullPointerException();
        }

        @DoesNotThrowException(
                reason="just returns constant, is final, may not be overridden",
                requires={}
        )
        final public int finalQux() {
            return 42;
        }
    }

    private final static class FooBar extends Foo {
        @Override
        @DoesNotThrowException(
            reason="just returns constant, class is final, may not be overridden",
            requires={}
        )
        public int baz() {
            return 42;
        }

        @Override
        @ExpectedExceptions()
        public int qux() {
            throw new NullPointerException();
        }
    }

    public static class PublicFooBar extends Foo {
        @Override
        @ExpectedExceptionsByOverridingMethods(
            reason = "just returns constant, is not final, class is public and not final"
        )
        public int baz() {
            return 42;
        }

        @Override
        @ExpectedExceptions()
        public int qux() {
            throw new NullPointerException();
        }

        @DoesNotThrowException(
                reason="just returns constant, is final, class is not final, may not be overridden",
                requires={}
        )
        public final int publicFinalQux() {
            return 42;
        }
    }

    public final static class PublicFinalFooBar extends Foo {
        @Override
        @DoesNotThrowException(
                reason="just returns constant, is not final, class is final, may not be overridden",
                requires={}
        )
        public int baz() {
            return 42;
        }

        @Override
        @ExpectedExceptions()
        public int qux() {
            throw new NullPointerException();
        }
    }

    @ExpectedExceptions(
        reason="allocates new object => may raise OutOfMemoryException"
    )
    public int noSubclasses() {
        FooBar foobar = new FooBar();
        return foobar.baz();
    }

    private final Foo foo = new Foo();
    private final FooBar foobar = new FooBar();
    private final Foo fooBar = new FooBar();

    @ExpectedExceptions()
    public int subclassThrows() {
        return foo.baz();
    }

    @ExpectedExceptions()
    public int superclassThrows() {
        return foo.qux();
    }

    @ExpectedExceptions()
    public int superclassThrows2() {
        return fooBar.qux();
    }
}
