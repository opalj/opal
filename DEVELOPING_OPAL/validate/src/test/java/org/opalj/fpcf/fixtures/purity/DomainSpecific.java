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
package org.opalj.fpcf.fixtures.purity;

import org.opalj.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.fpcf.analyses.purity.L2PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Test cases for purity in the presence of domain specific actions
 *
 * @author Dominik Helm
 */
public class DomainSpecific {

    public PrintStream stream = System.out;

    @Pure(value = "Only initializes stream with System.out",
            analyses = { L1PurityAnalysis.class, L2PurityAnalysis.class })
    @Impure(value = "Class is not immutable", analyses = L0PurityAnalysis.class)
    public DomainSpecific() {
    }

    @DomainSpecificPure("Potential DivisionByZeroException")
    @Impure(value = "Potential DivisionByZeroException", analyses = L0PurityAnalysis.class)
    /**@note: Analysis does not yet recognize implicit exceptions */
    public static int domainSpecificPure(int i) {
        return 1221 / i;
    }

    @DomainSpecificSideEffectFree("Potential DivisionByZeroException, uses non-final static value")
    @Impure(value = "Potential DivisionByZeroException", analyses = { L0PurityAnalysis.class })
    public static int domainSpecificSideEffectFree() {
        return 12121 % PrimitiveTypes.nonFinalStaticField;
    }

    @DomainSpecificContextuallyPure(
            value = "Potential DivisionByZeroException, synchronizes on receiver", modifies = {0})
    @Impure(value = "Synchronizes on receiver",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public synchronized int domainSpecificExternallyPure(int i) {
        return 1221 / i;
    }

    @DomainSpecificContextuallySideEffectFree(
            value = "PotentialDivisionByZeroException, modifies instance field stream",
            modifies = {0})
    @Impure(value = "Modifies instance field stream",
            analyses = { L0PurityAnalysis.class, L1PurityAnalysis.class })
    public int domainSpecificExternallySideEffectFree() {
        stream = System.err;
        return 12121 % PrimitiveTypes.nonFinalStaticField;
    }

    @DomainSpecificPure("Writes to System.out")
    @Impure(value = "Writes to System.out", analyses = L0PurityAnalysis.class)
    public static void pureSystemOut(String message) {
        PrintStream stream = System.out;
        if (stream != null) {
            stream.println(message);
        }
    }

    @DomainSpecificSideEffectFree("Writes to System.out, uses non-final static value")
    @Impure(value = "Writes to System.out", analyses = L0PurityAnalysis.class)
    public static void sideEffectFreeSystemOut() {
        PrintStream stream = System.out;
        if (stream != null) {
            stream.println(PrimitiveTypes.nonFinalStaticField);
        }
    }

    @Impure("Writes to an unspecific PrintStream")
    public static void impurePrintStream(PrintStream out, String message) {
        out.println(message);
    }

    @DomainSpecificPure("Uses java.util.logging")
    @Impure(value = "Uses java.util.logging", analyses = L0PurityAnalysis.class)
    public static void pureLogging(String message) {
        Logger log = Logger.getLogger("Test");
        if (log != null) {
            log.log(Level.WARNING, message);
        }
    }

    @DomainSpecificPure("Uses java.util.logging, LogManager/log could be null")
    @Impure(value = "Uses java.util.logging", analyses = L0PurityAnalysis.class)
    public static void pureLogging2(String message) {
        Logger log = LogManager.getLogManager().getLogger("Test");
        log.severe(message);
    }

    @DomainSpecificPure("Uses java.util.logging")
    @Impure(value = "Uses java.util.logging", analyses = L0PurityAnalysis.class)
    public void pureLogging3(Logger log, String message) {
        if (log != null) {
            log.info(message);
        }
    }

    @DomainSpecificPure("Raises explicit exception")
    @Impure(value = "Raises explicit exception", analyses = L0PurityAnalysis.class)
    public static void raisesExplicitException() {
        throw new RuntimeException();
    }

    @Impure(value = "Raises explicit exception with impure fillInStackTrace")
    public static void raisesImpureException() {
        throw new RuntimeException() {

            public int value = 0;

            public synchronized Throwable fillInStackTrace() {
                value = System.getenv().size();
                return this;
            }
        };
    }
}
