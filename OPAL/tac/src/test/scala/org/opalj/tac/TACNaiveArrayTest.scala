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
class TACNaiveArrayTest extends TACNaiveTest {

    val ArrayInstructionsType = ObjectType("tactest/ArrayCreationAndManipulation")

    val project = biProject("tactest-8-preserveAllLocals.jar")

    val ArrayInstructionsClassFile = project.classFile(ArrayInstructionsType).get

    val RefArrayMethod = ArrayInstructionsClassFile.findMethod("refArray").head
    val MultidimArrayMethod = ArrayInstructionsClassFile.findMethod("multidimArray").head
    val DoubleArrayMethod = ArrayInstructionsClassFile.findMethod("doubleArray").head
    val FloatArrayMethod = ArrayInstructionsClassFile.findMethod("floatArray").head
    val IntArrayMethod = ArrayInstructionsClassFile.findMethod("intArray").head
    val LongArrayMethod = ArrayInstructionsClassFile.findMethod("longArray").head
    val ShortArrayMethod = ArrayInstructionsClassFile.findMethod("shortArray").head
    val ByteArrayMethod = ArrayInstructionsClassFile.findMethod("byteArray").head
    val CharArrayMethod = ArrayInstructionsClassFile.findMethod("charArray").head

    describe("the naive TAC of array creation and manipulation instructions") {

        def expectedAST(
            cTpe:      ComputationalType,
            arrayType: ArrayType,
            const:     Expr[IdBasedVar]
        ) = Array(
            Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
            Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 5)),
            Assignment(1, SimpleVar(0, ComputationalTypeReference), NewArray(1, List(SimpleVar(0, ComputationalTypeInt)), arrayType)),
            Assignment(3, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
            Assignment(4, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            Assignment(5, SimpleVar(1, ComputationalTypeInt), IntConst(5, 4)),
            Assignment(6, SimpleVar(2, cTpe), const),
            ArrayStore(7, SimpleVar(0, ComputationalTypeReference), SimpleVar(1, ComputationalTypeInt), SimpleVar(2, cTpe)),
            Assignment(8, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
            Assignment(9, SimpleVar(1, ComputationalTypeInt), IntConst(9, 4)),
            Assignment(10, SimpleVar(0, cTpe), ArrayLoad(10, SimpleVar(1, ComputationalTypeInt), SimpleVar(0, ComputationalTypeReference))),
            Assignment(11, SimpleVar(-3, cTpe), SimpleVar(0, cTpe)),
            Return(12)
        )

        def expectedJLC(tpe: String, value: String) = Array[String](
            "0: r_0 = this",
            "1: op_0 = 5",
            "2: op_0 = new "+tpe+"[op_0]",
            "3: r_1 = op_0",
            "4: op_0 = r_1",
            "5: op_1 = 4",
            "6: op_2 = "+value+"",
            "7: op_0[op_1] = op_2",
            "8: op_0 = r_1",
            "9: op_1 = 4",
            "10: op_0 = op_0[op_1]",
            "11: r_2 = op_0",
            "12: return"
        ).mkString("\n")

        it("should correctly reflect reference array instructions") {
            val stmts = TACNaive(method = RefArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(Array(
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 5)),
                Assignment(1, SimpleVar(0, ComputationalTypeReference), NewArray(1, List(SimpleVar(0, ComputationalTypeInt)), ArrayType.ArrayOfObject)),
                Assignment(4, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Assignment(5, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(6, SimpleVar(1, ComputationalTypeInt), IntConst(6, 4)),
                Assignment(7, SimpleVar(2, ComputationalTypeReference), New(7, ObjectType.Object)),
                Nop(10),
                NonVirtualMethodCall(11, ObjectType.Object, false, "<init>", MethodDescriptor(NoFieldTypes, VoidType), SimpleVar(2, ComputationalTypeReference), List()),
                ArrayStore(14, SimpleVar(0, ComputationalTypeReference), SimpleVar(1, ComputationalTypeInt), SimpleVar(2, ComputationalTypeReference)),
                Assignment(15, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(16, SimpleVar(1, ComputationalTypeInt), IntConst(16, 4)),
                Assignment(17, SimpleVar(0, ComputationalTypeReference), ArrayLoad(17, SimpleVar(1, ComputationalTypeInt), SimpleVar(0, ComputationalTypeReference))),
                Assignment(18, SimpleVar(-3, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Return(19)
            ))
            val expected = Array(
                "0: r_0 = this",
                "1: op_0 = 5",
                "2: op_0 = new java.lang.Object[op_0]",
                "3: r_1 = op_0",
                "4: op_0 = r_1",
                "5: op_1 = 4",
                "6: op_2 = new java.lang.Object",
                "7: ;",
                "8: op_2/*(non-virtual) java.lang.Object*/.<init>()",
                "9: op_0[op_1] = op_2",
                "10: op_0 = r_1",
                "11: op_1 = 4",
                "12: op_0 = op_0[op_1]",
                "13: r_2 = op_0",
                "14: return"
            ).mkString("\n")

            javaLikeCode.shouldEqual(expected)
        }

        it("should correctly reflect multidimensional array instructions") {
            val stmts = TACNaive(method = MultidimArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(Array[Stmt[IdBasedVar]](
                Assignment(-1, SimpleVar(-1, ComputationalTypeReference), Param(ComputationalTypeReference, "this")),
                Assignment(0, SimpleVar(0, ComputationalTypeInt), IntConst(0, 4)),
                Assignment(1, SimpleVar(1, ComputationalTypeInt), IntConst(1, 2)),
                Assignment(2, SimpleVar(0, ComputationalTypeReference),
                    NewArray(2, List(SimpleVar(1, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)), ArrayType(ArrayType(IntegerType)))),
                Assignment(6, SimpleVar(-2, ComputationalTypeReference), SimpleVar(0, ComputationalTypeReference)),
                Assignment(7, SimpleVar(0, ComputationalTypeReference), SimpleVar(-2, ComputationalTypeReference)),
                Assignment(8, SimpleVar(0, ComputationalTypeInt), ArrayLength(8, SimpleVar(0, ComputationalTypeReference))),
                Assignment(9, SimpleVar(-3, ComputationalTypeInt), SimpleVar(0, ComputationalTypeInt)),
                Return(10)
            ))
            javaLikeCode.shouldEqual(Array(
                "0: r_0 = this",
                "1: op_0 = 4",
                "2: op_1 = 2",
                "3: op_0 = new int[op_0][op_1]",
                "4: r_1 = op_0",
                "5: op_0 = r_1",
                "6: op_0 = op_0.length",
                "7: r_2 = op_0",
                "8: return"
            ).mkString("\n"))
        }

        it("should correctly reflect double array instructions") {
            val stmts = TACNaive(method = DoubleArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeDouble, ArrayType(DoubleType), DoubleConst(6, 1.0d)))
            javaLikeCode.shouldEqual(expectedJLC("double", "1.0d"))
        }

        it("should correctly reflect float array instructions") {
            val stmts = TACNaive(method = FloatArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeFloat, ArrayType(FloatType), FloatConst(6, 2.0f)))
            javaLikeCode.shouldEqual(expectedJLC("float", "2.0f"))
        }

        it("should correctly reflect int array instructions") {
            val stmts = TACNaive(method = IntArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeInt, ArrayType(IntegerType), IntConst(6, 2)))
            javaLikeCode.shouldEqual(expectedJLC("int", "2"))
        }

        it("should correctly reflect long array instructions") {
            val stmts = TACNaive(method = LongArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeLong, ArrayType(LongType), LongConst(6, 1)))
            javaLikeCode.shouldEqual(expectedJLC("long", "1l"))
        }

        it("should correctly reflect short array instructions") {
            val stmts = TACNaive(method = ShortArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeInt, ArrayType(ShortType), IntConst(6, 2)))
            javaLikeCode.shouldEqual(expectedJLC("short", "2"))
        }

        it("should correctly reflect byte array instructions") {
            val stmts = TACNaive(method = ByteArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeInt, ArrayType(ByteType), IntConst(6, 2)))
            javaLikeCode.shouldEqual(expectedJLC("byte", "2"))
        }

        it("should correctly reflect char array instructions") {
            val stmts = TACNaive(method = CharArrayMethod, classHierarchy = ClassHierarchy.PreInitializedClassHierarchy).stmts
            val javaLikeCode = ToTxt.stmtsToTxtStmt(stmts, false)

            assert(stmts.nonEmpty)
            assert(javaLikeCode.length > 0)
            stmts.shouldEqual(expectedAST(ComputationalTypeInt, ArrayType(CharType), IntConst(6, 2)))
            javaLikeCode.shouldEqual(expectedJLC("char", "2"))
        }
    }
}
