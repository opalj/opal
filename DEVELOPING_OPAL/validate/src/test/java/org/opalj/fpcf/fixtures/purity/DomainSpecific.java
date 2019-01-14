/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.fixtures.purity;

import org.opalj.br.fpcf.analyses.L0PurityAnalysis;
import org.opalj.fpcf.properties.purity.*;
import org.opalj.tac.fpcf.analyses.purity.L1PurityAnalysis;
import org.opalj.tac.fpcf.analyses.purity.L2PurityAnalysis;

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
