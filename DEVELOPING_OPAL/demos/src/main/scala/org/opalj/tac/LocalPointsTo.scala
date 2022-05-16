/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.FileInputStream

import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.ClassHierarchy
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.reader.Java9Framework

/**
 * Collects points-to information related to a method.
 *
 * @author Michael Eichberg
 */
object LocalPointsTo {

    def main(args: Array[String]): Unit = {
        // Load the class file (we don't handle invokedynamic in this case)
        val cf = Java9Framework.ClassFile(() => new FileInputStream(args(0))).head
        // ... now let's take the first method that matches our filter
        val m = cf.methods.filter(m => m.signatureToJava().contains(args(1))).head
        // ... let's get one of the default pre-initialized class hierarchies (typically we want a project!)
        val ch = ClassHierarchy.PreInitializedClassHierarchy
        // ... perform the data-flow analysis
        val aiResult = BaseAI(m, new PrimitiveTACAIDomain(ch, m))
        // now, we can transform the bytecode to three-address code
        val tac = TACAI(m, ch, aiResult, propagateConstants = true)(Nil /* no optimizations */ )

        // Let's print the three address code to get a better feeling for it...
        // Please note, "pc" in the output refers to the program counter of the original
        // underlying bytecode instruction; the "pc" is kept to avoid that we have to
        // transform the other information in the class file too.
        println(tac)

        // Let's collect the information where a reference value that is passed
        // to some method is coming from.
        for {
            (MethodCallParameters(params), stmtIndex) <- tac.stmts.iterator.zipWithIndex
            (UVar(v, defSites), paramIndex) <- params.iterator.zipWithIndex
            if v.computationalType == ComputationalTypeReference
            defSite <- defSites
        } {
            if (defSite >= 0) {
                val Assignment(_, _, expr) = tac.stmts(defSite) // a def site is always an assignment
                println(s"call@$stmtIndex(param=$paramIndex) is "+expr)
            } else {
                println(s"call@$stmtIndex(param=$paramIndex) takes param "+(-defSite - 1))
            }
        }

        println("Done.")
    }
}
