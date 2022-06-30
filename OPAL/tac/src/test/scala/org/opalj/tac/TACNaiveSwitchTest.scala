/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith
import org.opalj.collection.immutable.IntIntPair
import org.opalj.br._
import org.opalj.br.TestSupport.biProject

import scala.collection.immutable.ArraySeq

/**
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveSwitchTest extends TACNaiveTest {

    val SwitchStatementsType = ObjectType("tactest/SwitchStatements")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val SwitchStatementsClassFile = project.classFile(SwitchStatementsType).get

    val TableSwitchMethod = SwitchStatementsClassFile.findMethod("tableSwitch").head
    val LookupSwitchMethod = SwitchStatementsClassFile.findMethod("lookupSwitch").head

    describe("the naive TAC of switch instructions") {
        it("should correctly reflect tableswitch case") {
            val statements = TACNaive(method = TableSwitchMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, true)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)

            val expected = Array(
                Assignment(
                    -1,
                    SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")
                ),
                Assignment(
                    -1,
                    SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")
                ),
                Assignment(
                    0,
                    SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)
                ),
                Switch(
                    1,
                    10,
                    SimpleVar(0, ComputationalTypeInt),
                    ArraySeq(IntIntPair(1, 4), IntIntPair(2, 6), IntIntPair(3, 8))
                ),
                Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 1)),
                ReturnValue(29, SimpleVar(0, ComputationalTypeInt)),
                Assignment(30, SimpleVar(0, ComputationalTypeInt), IntConst(30, 2)),
                ReturnValue(31, SimpleVar(0, ComputationalTypeInt)),
                Assignment(32, SimpleVar(0, ComputationalTypeInt), IntConst(32, 3)),
                ReturnValue(33, SimpleVar(0, ComputationalTypeInt)),
                Assignment(34, SimpleVar(0, ComputationalTypeInt), IntConst(34, 0)),
                ReturnValue(35, SimpleVar(0, ComputationalTypeInt))
            )
            compareStatements(
                ArraySeq.unsafeWrapArray(expected),
                ArraySeq.unsafeWrapArray(statements)
            )

            javaLikeCode.shouldEqual(Array(
                "0:/*pc=-1:*/ r_0 = this",
                "1:/*pc=-1:*/ r_1 = p_1",
                "2:/*pc=0:*/ op_0 = r_1",
                "3:/*pc=1:*/ switch(op_0){\n    1: goto 4;\n    2: goto 6;\n    3: goto 8;\n    default: goto 10\n}",
                "4:/*pc=28:*/ op_0 = 1",
                "5:/*pc=29:*/ return op_0",
                "6:/*pc=30:*/ op_0 = 2",
                "7:/*pc=31:*/ return op_0",
                "8:/*pc=32:*/ op_0 = 3",
                "9:/*pc=33:*/ return op_0",
                "10:/*pc=34:*/ op_0 = 0",
                "11:/*pc=35:*/ return op_0"
            ).mkString("\n"))
        }

        it("should correctly reflect lookupswitch case") {
            val statements = TACNaive(method = LookupSwitchMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, true)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(Array(
                Assignment(
                    -1,
                    SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")
                ),
                Assignment(
                    -1,
                    SimpleVar(-2, ComputationalTypeInt), Param(ComputationalTypeInt, "p_1")
                ),
                Assignment(
                    0,
                    SimpleVar(0, ComputationalTypeInt), SimpleVar(-2, ComputationalTypeInt)
                ),
                Switch(
                    1,
                    8,
                    SimpleVar(0, ComputationalTypeInt),
                    ArraySeq(IntIntPair(1, 4), IntIntPair(10, 6))
                ),
                Assignment(28, SimpleVar(0, ComputationalTypeInt), IntConst(28, 10)),
                ReturnValue(30, SimpleVar(0, ComputationalTypeInt)),
                Assignment(31, SimpleVar(0, ComputationalTypeInt), IntConst(31, 200)),
                ReturnValue(34, SimpleVar(0, ComputationalTypeInt)),
                Assignment(35, SimpleVar(0, ComputationalTypeInt), IntConst(35, 0)),
                ReturnValue(36, SimpleVar(0, ComputationalTypeInt))
            ))
            javaLikeCode.shouldEqual(Array(
                "0:/*pc=-1:*/ r_0 = this",
                "1:/*pc=-1:*/ r_1 = p_1",
                "2:/*pc=0:*/ op_0 = r_1",
                "3:/*pc=1:*/ switch(op_0){\n    1: goto 4;\n    10: goto 6;\n    default: goto 8\n}",
                "4:/*pc=28:*/ op_0 = 10",
                "5:/*pc=30:*/ return op_0",
                "6:/*pc=31:*/ op_0 = 200",
                "7:/*pc=34:*/ return op_0",
                "8:/*pc=35:*/ op_0 = 0",
                "9:/*pc=36:*/ return op_0"
            ).mkString("\n"))
        }

    }
}
