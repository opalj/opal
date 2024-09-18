/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ifds

/**
 * Interprocedural control flow graph for Java programs in forward direction
 */
class JavaForwardICFG(project: SomeProject) extends JavaBaseICFG {
    // TODO (IDE) CURRENTLY DEPENDS ON IMPLEMENTATION FROM IFDS
    private val baseICFG = new ifds.JavaForwardICFG(project)

    override def getStartStatements(callable: Method): collection.Set[JavaStatement] =
        baseICFG.startStatements(callable).map {
            case org.opalj.tac.fpcf.analyses.ifds.JavaStatement(method, index, code, cfg) =>
                JavaStatement(method, index, isReturnNode = false, code, cfg)
        }

    override def getNextStatements(stmt: JavaStatement): collection.Set[JavaStatement] = {
        if (isCallStatement(stmt)) {
            immutable.Set(JavaStatement(stmt.method, stmt.index, isReturnNode = true, stmt.code, stmt.cfg))
        } else {
            baseICFG.nextStatements(
                org.opalj.tac.fpcf.analyses.ifds.JavaStatement(stmt.method, stmt.index, stmt.code, stmt.cfg)
            ).map {
                case org.opalj.tac.fpcf.analyses.ifds.JavaStatement(method, index, code, cfg) =>
                    JavaStatement(method, index, isReturnNode = false, code, cfg)
            }
        }
    }

    // TODO (IDE) REFACTOR AS 'getCallees(...): Set[Method]'
    override def getCalleesIfCallStatement(stmt: JavaStatement): Option[collection.Set[Method]] = {
        if (stmt.isReturnNode) {
            None
        } else {
            val calleesOption = baseICFG.getCalleesIfCallStatement(
                org.opalj.tac.fpcf.analyses.ifds.JavaStatement(stmt.method, stmt.index, stmt.code, stmt.cfg)
            )
            calleesOption match {
                case None => None
                case Some(callees) =>
                    if (callees.isEmpty) {
                        throw new IllegalStateException(
                            s"Statement ${stringifyStatement(stmt)} is detected as call statement but no callees were found!"
                        )
                    } else {
                        Some(callees)
                    }
            }
        }
    }

    override def getCallablesCallableFromOutside: collection.Set[Method] = {
        baseICFG.methodsCallableFromOutside.map { declaredMethod => declaredMethod.asDefinedMethod.definedMethod }
    }
}
