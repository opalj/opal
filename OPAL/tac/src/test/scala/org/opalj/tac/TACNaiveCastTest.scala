/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.br._
import org.opalj.br.TestSupport.biProject

/**
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveCastTest extends TACNaiveTest {

    describe("the naive TAC of cast instructions") {

        val CastInstructionsType = ObjectType("tactest/CastInstructions")

        val project = biProject("tactest-8-preserveAllLocals.jar")

        val CastInstructionsClassFile = project.classFile(CastInstructionsType).get

        val TypecheckStringMethod = CastInstructionsClassFile.findMethod("typecheckString").head
        val TypecheckListMethod = CastInstructionsClassFile.findMethod("typecheckList").head
        val CheckcastMethod = CastInstructionsClassFile.findMethod("checkcast").head

        val D2FMethod = CastInstructionsClassFile.findMethod("d2f").head
        val D2LMethod = CastInstructionsClassFile.findMethod("d2l").head
        val D2IMethod = CastInstructionsClassFile.findMethod("d2i").head

        val F2DMethod = CastInstructionsClassFile.findMethod("f2d").head
        val F2IMethod = CastInstructionsClassFile.findMethod("f2i").head
        val F2LMethod = CastInstructionsClassFile.findMethod("f2l").head

        val L2DMethod = CastInstructionsClassFile.findMethod("l2d").head
        val L2IMethod = CastInstructionsClassFile.findMethod("l2i").head
        val L2FMethod = CastInstructionsClassFile.findMethod("l2f").head

        val I2DMethod = CastInstructionsClassFile.findMethod("i2d").head
        val I2LMethod = CastInstructionsClassFile.findMethod("i2l").head
        val I2FMethod = CastInstructionsClassFile.findMethod("i2f").head
        val I2SMethod = CastInstructionsClassFile.findMethod("i2s").head
        val I2BMethod = CastInstructionsClassFile.findMethod("i2b").head
        val I2CMethod = CastInstructionsClassFile.findMethod("i2c").head

        def longResultJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            strg,
            "4: r_3 = op_0",
            "5: return"
        ).mkString("\n")

        def shortResultJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            strg,
            "4: r_2 = op_0",
            "5: return"
        ).mkString("\n")

        def typecheckResultJLC(strg: String) = Array(
            "0: r_0 = this",
            "1: r_1 = p_1",
            "2: op_0 = r_1",
            "3: op_0 = op_0 instanceof "+strg+"",
            "4: r_2 = op_0",
            "5: return"
        ).mkString("\n")

        def castResultAST(from: ComputationalType, to: BaseType) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, from), Param(from, "p_1")),
            Assignment(0, SimpleVar(0, from), SimpleVar(-2, from)),
            Assignment(1, SimpleVar(0, to.computationalType), PrimitiveTypecastExpr(1, to, SimpleVar(0, from))),
            Assignment(2, SimpleVar(-2 - from.category.id, to.computationalType), SimpleVar(0, to.computationalType)),
            Return(3)
        )

        def typecheckResultAST(refTp: ReferenceType) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
            Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            Assignment(1, SimpleVar(0, ComputationalTypeInt), InstanceOf(1, SimpleVar(0, ComputationalTypeReference), refTp)),
            Assignment(4, SimpleVar(-3, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
            Return(5)
        )

        it("should correctly reflect the instanceof Object instruction") {
            val statements = TACNaive(method = TypecheckStringMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(typecheckResultAST(ObjectType.Object))
            javaLikeCode.shouldEqual(typecheckResultJLC(ObjectType.Object.toJava))
        }

        it("should correctly reflect the instanceof List instruction") {
            val statements = TACNaive(method = TypecheckListMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            val listTpe = ReferenceType.apply("java/util/List")
            statements.shouldEqual(typecheckResultAST(listTpe))
            javaLikeCode.shouldEqual(typecheckResultJLC(listTpe.toJava))
        }

        it("should correctly reflect the checkcast instruction") {
            val statements = TACNaive(method = CheckcastMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            val listType = ReferenceType("java/util/List")
            statements.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(-1, SimpleVar(-2, ComputationalTypeReference), Param(ComputationalTypeReference, "p_1")),
                Assignment(0, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Checkcast(1, SimpleVar(0, ComputationalTypeReference), listType),
                Assignment(4, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Return(5)
            ))
            javaLikeCode shouldEqual Array(
                "0: r_0 = this",
                "1: r_1 = p_1",
                "2: op_0 = r_1",
                s"3: (${listType.toJava}) op_0",
                "4: r_2 = op_0",
                "5: return"
            ).mkString("\n")
        }

        it("should correctly reflect the d2f instruction") {
            val statements = TACNaive(method = D2FMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeDouble, FloatType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (float) op_0"))
        }

        it("should correctly reflect the d2i instruction") {
            val statements = TACNaive(method = D2IMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeDouble, IntegerType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (int) op_0"))
        }

        it("should correctly reflect the d2l instruction") {
            val statements = TACNaive(method = D2LMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeDouble, LongType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (long) op_0"))
        }

        it("should correctly reflect the f2d instruction") {
            val statements = TACNaive(method = F2DMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeFloat, DoubleType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (double) op_0"))
        }

        it("should correctly reflect the f2l instruction") {
            val statements = TACNaive(method = F2LMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeFloat, LongType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (long) op_0"))
        }

        it("should correctly reflect the f2i instruction") {
            val statements = TACNaive(method = F2IMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeFloat, IntegerType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (int) op_0"))
        }

        it("should correctly reflect the l2d instruction") {
            val statements = TACNaive(method = L2DMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeLong, DoubleType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (double) op_0"))
        }

        it("should correctly reflect the l2f instruction") {
            val statements = TACNaive(method = L2FMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeLong, FloatType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (float) op_0"))
        }

        it("should correctly reflect the l2i instruction") {
            val statements = TACNaive(method = L2IMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeLong, IntegerType))
            javaLikeCode.shouldEqual(longResultJLC("3: op_0 = (int) op_0"))
        }

        it("should correctly reflect the i2d instruction") {
            val statements = TACNaive(method = I2DMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, DoubleType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (double) op_0"))
        }

        it("should correctly reflect the i2l instruction") {
            val statements = TACNaive(method = I2LMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, LongType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (long) op_0"))
        }

        it("should correctly reflect the i2f instruction") {
            val statements = TACNaive(method = I2FMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, FloatType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (float) op_0"))
        }

        it("should correctly reflect the i2c instruction") {
            val statements = TACNaive(method = I2CMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, CharType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (char) op_0"))
        }

        it("should correctly reflect the i2b instruction") {
            val statements = TACNaive(method = I2BMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, ByteType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (byte) op_0"))
        }

        it("should correctly reflect the i2s instruction") {
            val statements = TACNaive(method = I2SMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(statements, false)

            assert(statements.nonEmpty)
            assert(javaLikeCode.length > 0)
            statements.shouldEqual(castResultAST(ComputationalTypeInt, ShortType))
            javaLikeCode.shouldEqual(shortResultJLC("3: op_0 = (short) op_0"))
        }
    }
}
