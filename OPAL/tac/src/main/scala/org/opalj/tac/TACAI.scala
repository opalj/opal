/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.annotation.switch
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.PCAndAnyRef
import org.opalj.br.BaseType
import org.opalj.br.ArrayType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.FloatType
import org.opalj.br.CharType
import org.opalj.br.DoubleType
import org.opalj.br.ShortType
import org.opalj.br.ByteType
import org.opalj.br.Code
import org.opalj.br.ClassHierarchy
import org.opalj.br.ComputationalTypeInt
import org.opalj.br.instructions._
import org.opalj.br.cfg.CFG
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.ai.BaseAI
import org.opalj.ai.AIResult
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.collection.immutable.IntIntPair
import org.opalj.tac.JSR

import scala.collection.immutable.ArraySeq
import scala.collection.mutable

/**
 * Factory to convert the bytecode of a method into a three address representation using the
 * results of a(n) (local) abstract interpretation of the method.
 *
 * The generated representation is completely parameterized over the domains that were used
 * to perform the abstract interpretation. The only requirement is that the Def/Use information
 * is recorded while performing the abstract interpretation (see
 * [[org.opalj.ai.domain.RecordDefUse]]).
 *
 * The generated representation is necessarily in static single assignment form: each variable
 * is assigned exactly once, and every variable is defined before it is used. However, no PHI
 * instructions are inserted; instead - in case of a use - we simply directly refer to all
 * def sites.
 *
 * @author Michael Eichberg
 */
object TACAI {

    /**
     * Returns a map which maps an ai-based value origin for a parameter to the tac value origin;
     * to lookup the tac based origin, the ai-based origin aiVO has to be negated and 1 has to
     * be subtracted.
     *
     * @return An implicit map (keys are aiVOKey = `-aiVo-1`) from `aiVO` to `tacVo`.
     */
    def normalizeParameterOriginsMap(
        descriptor: MethodDescriptor,
        isStatic:   Boolean
    ): Array[Int] = {
        val parameterTypes = descriptor.parameterTypes
        val parametersCount = descriptor.parametersCount

        // we need this `map` only temporarily; and this is always large enough...
        val aiVOToTACVo = new Array[Int](parametersCount * 2 + 1)
        var aiVO = -1 // initialized for the static method case
        var tacVO = -2 // initialized for the static method case

        if (!isStatic) {
            aiVOToTACVo(0) = -1 // basically vo -1 is mapped to tacVO -1
            aiVO = -2
        }

        var i = 0 // initialized for the static method case
        while (i < parametersCount) {
            aiVOToTACVo(-aiVO - 1) = tacVO
            tacVO -= 1
            aiVO -= parameterTypes(i).operandSize
            i += 1
        }

        aiVOToTACVo
    }

    private[this] final val NoParameters = new Parameters(new Array[DUVar[_]](0))

    def apply(
        project: SomeProject,
        method:  Method
    )(
        domain: Domain with RecordDefUse = new DefaultDomainWithCFGAndDefUse(project, method)
    ): AITACode[TACMethodParameter, domain.DomainValue] = {
        val aiResult = BaseAI(method, domain)
        TACAI(project, method, aiResult)
    }

    def apply(
        project:  SomeProject,
        method:   Method,
        aiResult: AIResult { val domain: Domain with RecordDefUse }
    ): AITACode[TACMethodParameter, aiResult.domain.DomainValue] = {
        val config = project.config
        val propagateConstants = config.getBoolean("org.opalj.tacai.performConstantPropagation")
        TACAI(method, project.classHierarchy, aiResult, propagateConstants)(Nil)
    }

    /**
     * Converts the bytecode of a method into a three address representation using
     * the result of an abstract interpretation.
     *
     * @param   method A method with a body. I.e., a non-native, non-abstract method.
     * @param   aiResult The result of the abstract interpretation of the respective method.
     * @param   optimizations The transformations that should be executed (NoOptimizations
     *          is always possible).
     * @return  The array with the generated statements.
     */
    def apply(
        method:             Method,
        classHierarchy:     ClassHierarchy,
        aiResult:           AIResult { val domain: Domain with RecordDefUse },
        propagateConstants: Boolean
    )(
        optimizations: List[TACOptimization[TACMethodParameter, DUVar[aiResult.domain.DomainValue], AITACode[TACMethodParameter, aiResult.domain.DomainValue]]]
    ): AITACode[TACMethodParameter, aiResult.domain.DomainValue] = {

        if (aiResult.wasAborted)
            throw new IllegalArgumentException("cannot create TACAI from aborted AI result")

        val domain: aiResult.domain.type = aiResult.domain
        val operandsArray: aiResult.domain.OperandsArray = aiResult.operandsArray
        val localsArray: aiResult.domain.LocalsArray = aiResult.localsArray

        def allDead(fromPC: Int, untilPC: Int): Boolean = {
            val a = operandsArray
            var i = fromPC
            while (i < untilPC) {
                if (a(i) ne null) return false;
                i += 1
            }
            true
        }

        import UnaryArithmeticOperators._
        import BinaryArithmeticOperators._
        import RelationalOperators._

        val isStatic = method.isStatic
        val descriptor = method.descriptor
        val code = aiResult.code // THIS IS NOT `method.body.get` IF THE BODY IS INVALID!
        import code.pcOfNextInstruction
        val instructions: Array[Instruction] = code.instructions
        val codeSize: Int = instructions.length
        val cfg: CFG[Instruction, Code] = domain.bbCFG

        def wasExecuted(pc: Int) = operandsArray(pc) != null

        // We already have the def-use information directly available, hence, for
        // instructions such as swap and dup, which do not create "relevant"
        // uses, we do not have to create multiple instructions, therefore, we
        // can directly create an array which will definitively be able to hold the
        // "final list" of statements which will include nops for all useless instructions
        // and may not be fully utilized.
        // Please note, that we may require some space for storing handled exceptions!
        val maxStatements = codeSize + code.exceptionHandlers.size
        val statements = new Array[Stmt[DUVar[aiResult.domain.DomainValue]]](maxStatements)

        // pcToIndex is used for two things: (1) to update jump targets and debug information
        // which reference pcs and (2) to update def-use information.
        // In the first case, if an instruction is added before some instruction and both
        // instructions strictly belong together, then the index will point to the instruction
        // which was added. For example, the instruction to initialize the variable which stores
        // a caught exception will be the first instruction of the basic block and, hence,
        // has to be used in the corresponding tables etc.
        // For (2) we have to adapt the use-site if we have a self-use; the latter
        // happens if we have an instruction which immediately processes a caught exception. In
        // that case the use information associated with the def-site, which initializes the
        // the variable which stores the exception (`CaughtException`, would otherwise "use" itself.
        val pcToIndex = new Array[Int](codeSize + 1 /* +1 if the try includes the last inst. */ )

        val simpleRemapping = !descriptor.hasComputationalTypeCategory2ValueInInit
        // A function to map an ai based value origin (vo) of a __parameter__ to a tac origin.
        // To get the target value the ai based vo has to be negated and we have to add -1.
        // E.g., if the ai-based vo is -1 then the index which needs to be used is -(-1)-1 ==> 0
        // which will contain (for -1 only) the value -1.
        val normalizeParameterOrigins: IntTrieSet => IntTrieSet = {
            if (!isStatic && simpleRemapping) {
                // => no remapping is necessary
                (aiVOs: IntTrieSet) => aiVOs
            } else if (isStatic && simpleRemapping) {
                // => we have to subtract -1 from origins related to parameters
                (aiVOs: IntTrieSet) =>
                    aiVOs map { aiVO =>
                        if (aiVO >= 0 || aiVO <= ImmediateVMExceptionsOriginOffset)
                            aiVO
                        else
                            aiVO - 1
                    }
            } else {
                // => we create an array which contains the mapping information
                val aiVOToTACVo: Array[Int] = normalizeParameterOriginsMap(descriptor, isStatic)
                (aiVOs: IntTrieSet) => {
                    if (aiVOs eq null) {
                        IntTrieSet.empty
                    } else {
                        aiVOs map { aiVO =>
                            if (aiVO >= 0 || aiVO <= ImmediateVMExceptionsOriginOffset) {
                                aiVO
                            } else {
                                aiVOToTACVo(-aiVO - 1)
                            }
                        }
                    }
                }
            }
        }

        // The list of bytecode instructions which were killed (=>NOP), and for which we now
        // have to clear the usages.
        // Basically a mapping from a UseSite(PC) to a DefSite.
        val obsoleteUseSites = new mutable.ArrayDeque[PCAndAnyRef[IntTrieSet /*DefSites*/ ]]

        def killOperandBasedUsages(useSitePC: Int, valuesCount: Int): Unit = {
            if (valuesCount == 0)
                return ;

            // The value(s) is (are) not used and the expression is side effect free;
            // we now have to kill the usages to avoid "wrong" links.
            // E.g.,
            //   x: ArrayLength
            // x+1: NewArray // <= dead
            // now the def-site x would point to the use-site x+1, but this
            // site is removed and - therefore - this link from x to x+1 has to
            // be removed.
            import domain.operandOrigin
            var origins = normalizeParameterOrigins(operandOrigin(useSitePC, 0))
            var i = 1
            while (i < valuesCount) {
                origins ++= normalizeParameterOrigins(operandOrigin(useSitePC, i))
                i += 1
            }
            obsoleteUseSites.append(new PCAndAnyRef(useSitePC, origins))
        }

        def killRegisterBasedUsages(useSitePC: Int, index: Int): Unit = {
            val origins = normalizeParameterOrigins(domain.localOrigin(useSitePC, index))
            obsoleteUseSites.append(new PCAndAnyRef(useSitePC, origins))
        }

        // The catch handler statements which were added to the code that do not take up
        // the slot of an empty load/store statement.
        var addedHandlerStmts: IntTrieSet = IntTrieSet.empty

        /*
        val handlerPCs: IntArraySet = {
            val ehs = code.exceptionHandlers
            val handlerPCs = new IntArraySetBuilder(ehs.length)
            ehs.foreach(eh => handlerPCs += eh.handlerPC)
            handlerPCs.result()
        }
        */
        val handlerPCs: Array[Boolean] = new Array(codeSize)
        code.exceptionHandlers.foreach(eh => handlerPCs(eh.handlerPC) = true)

        var pc: Int = 0
        var index: Int = 0
        do {
            val nextPC = pcOfNextInstruction(pc)
            val instruction = instructions(pc)
            val opcode = instruction.opcode

            // -------------------------------------------------------------------------------------
            // ALL STATEMENTS ARE ADDED TO THE ARRAY BY THE FOLLOWING THREE FUNCTIONS:
            //
            //

            // The exception handler initializer is an extra instruction!
            // In very weird cases, where the first instruction of a handler is also
            // a jump target, the new "caught exception" becomes the target, however,
            // given that this instruction has no special side effects, this is not
            // problematic.
            //val addExceptionHandlerInitializer = handlerPCs.contains(pc)
            val addExceptionHandlerInitializer = handlerPCs(pc)

            def addStmt(stmt: Stmt[DUVar[aiResult.domain.DomainValue]]): Unit = {
                if (cfg.bb(pc).startPC != pc && statements(index - 1).astID == Nop.ASTID) {
                    // ... we are not at the beginning of a basic block, but the previous
                    // instruction was a NOP instruction... let's replace it by this instruction.
                    statements(index - 1) = stmt
                    pcToIndex(pc) = index - 1
                } else {
                    statements(index) = stmt
                    if (addExceptionHandlerInitializer) {
                        addedHandlerStmts += (index - 1)
                    } else {
                        pcToIndex(pc) = index
                    }
                    index += 1
                }
            }

            def addNOP(pcHint: Int): Unit = {
                if (addExceptionHandlerInitializer)
                    // We do not have to add the NOP, because the code to initialize the
                    // variable which references the exception is already added.
                    return ;

                // We only add a NOP if it is the first instruction of a basic block since
                // we want to ensure that we don't have to rewrite the CFG during the initial
                // transformation
                if (cfg.bb(pc).startPC == pc) {
                    statements(index) = Nop(pcHint)
                    pcToIndex(pc) = index
                    index += 1
                } else {
                    pcToIndex(pc) = -1
                }
            }

            // ADD AN EXPLICIT INSTRUCTION WHICH REPRESENTS THE CATCH HANDLER
            if (addExceptionHandlerInitializer) {
                val normalizedDefSites = normalizeParameterOrigins(domain.operandOrigin(pc, 0))
                val catchType = code.exceptionHandlers.find(_.handlerPC == pc).get.catchType
                statements(index) = CaughtException(pc, catchType, normalizedDefSites)
                pcToIndex(pc) = index
                index += 1
            }

            //
            //
            // ALL STATEMENTS ARE ADDED TO THE ARRAY BY THE PREVIOUS THREE FUNCTIONS!
            // -------------------------------------------------------------------------------------

            def addNOPAndKillOperandBasedUsages(valuesCount: Int): Unit = {
                killOperandBasedUsages(pc, valuesCount)
                addNOP(-pc - 1)
            }

            /**
             * Creates a local variable using the current pc and the type
             * information from the domain value if the local variable is used; otherwise
             * an expression statement or a nop statement is added.
             */
            def addInitLocalValStmt(
                pc:   Int,
                v:    aiResult.domain.DomainValue,
                expr: Expr[DUVar[aiResult.domain.DomainValue]]
            ): Unit = {
                val usedBy = domain.usedBy(pc)
                if (usedBy ne null) {
                    // assert(usedBy.forall(_ >= 0)) // internal consistency only
                    val localVal = DVar(aiResult.domain)(pc, v, usedBy)
                    addStmt(Assignment(pc, localVal, expr))
                } else if (expr.isSideEffectFree) {
                    if (instruction.opcode == IINC.opcode) {
                        val IINC(index, _) = instruction
                        killRegisterBasedUsages(pc, index)
                        addNOP(-pc - 1)
                    } else {
                        addNOPAndKillOperandBasedUsages(expr.subExprCount)
                    }
                } else {
                    addStmt(ExprStmt(pc, expr))
                }
            }

            def operandUse(index: Int): UVar[aiResult.domain.DomainValue] = {
                val operands = operandsArray(pc)
                // get the definition site; recall: negative pcs refer to parameters
                val defSites = normalizeParameterOrigins(domain.operandOrigin(pc, index))
                UVar(aiResult.domain)(operands(index), defSites)
            }

            def registerUse(index: Int): UVar[aiResult.domain.DomainValue] = {
                val locals = localsArray(pc)
                // get the definition site; recall: negative pcs refer to parameters
                val defSites = normalizeParameterOrigins(domain.localOrigin(pc, index))
                UVar(aiResult.domain)(locals(index), defSites)
            }

            def useOperands(operandsCount: Int): ArraySeq[UVar[aiResult.domain.DomainValue]] = {
                val ops = new Array[UVar[aiResult.domain.DomainValue]](operandsCount)
                var i = 0
                while (i < operandsCount) {
                    ops(i) = operandUse(i)
                    i += 1
                }
                ArraySeq.unsafeWrapArray(ops)
            }

            def arrayLoad(): Unit = {
                val index = operandUse(0)
                val arrayRef = operandUse(1)
                // to get the precise type we take a look at the next instruction's
                // top operand value
                val source = ArrayLoad(pc, index, arrayRef)
                if (wasExecuted(nextPC)) {
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, source)
                } else {
                    addStmt(ExprStmt(pc, source))
                }
            }

            def binaryArithmeticOperation(operator: BinaryArithmeticOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val cTpe = instruction.asArithmeticInstruction.computationalType
                val binExpr = BinaryExpr(pc, cTpe, operator, value1, value2)
                // may fail in case of a div by zero...
                if (wasExecuted(nextPC)) {
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, binExpr)
                } else {
                    addStmt(ExprStmt(pc, binExpr))
                }

            }

            @inline def prefixArithmeticOperation(operator: UnaryArithmeticOperator): Unit = {
                val value = operandUse(0)
                val cTpe = operandsArray(nextPC).head.computationalType
                val preExpr = PrefixExpr(pc, cTpe, operator, value)
                addInitLocalValStmt(pc, operandsArray(nextPC).head, preExpr)
            }

            @inline def primitiveCastOperation(targetTpe: BaseType): Unit = {
                val value = operandUse(0)
                val castExpr = PrimitiveTypecastExpr(pc, targetTpe, value)
                addInitLocalValStmt(pc, operandsArray(nextPC).head, castExpr)
            }

            @inline def newArray(arrayType: ArrayType): Unit = {
                // TODO move code for constant propagation directly to operandUse to enable transparent constant propagation everywhere! After that, the code should be: val count = operandUse(0); val newArray...
                val countUVar = operandUse(0)
                val countDV = countUVar.value.asPrimitiveValue
                val count =
                    if (propagateConstants && countDV.constantValue.isDefined) {
                        killOperandBasedUsages(pc, 1)
                        // We have to use the pc of the current newArray instruction and cannot
                        // use the pc of a def site because, if we have multiple def-sites,
                        // the information would become incomplete; using this approach it seems
                        // as if the constant was defined as a parameter.
                        IntConst(pc, countDV.asConstantInteger)
                    } else {
                        countUVar
                    }
                val newArray = NewArray(pc, List(count), arrayType)
                addInitLocalValStmt(pc, operandsArray(nextPC).head, newArray)
            }

            def loadConstant(instr: LoadConstantInstruction[_]): Unit = {
                instr match {
                    case LDCInt(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, IntConst(pc, value))

                    case LDCFloat(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, FloatConst(pc, value))

                    case LDCClass(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, ClassConst(pc, value))

                    case LDCString(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, StringConst(pc, value))

                    case LDCMethodHandle(value) =>
                        val lVal = operandsArray(nextPC).head
                        addInitLocalValStmt(pc, lVal, MethodHandleConst(pc, value))

                    case LDCMethodType(value) =>
                        val lVal = operandsArray(nextPC).head
                        addInitLocalValStmt(pc, lVal, MethodTypeConst(pc, value))

                    case LoadDouble(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, DoubleConst(pc, value))

                    case LoadLong(value) =>
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, LongConst(pc, value))

                    case LDCDynamic(bootstrapMethod, name, descriptor) =>
                        addInitLocalValStmt(
                            pc,
                            operandsArray(nextPC).head,
                            DynamicConst(pc, bootstrapMethod, name, descriptor)
                        )

                    case _ =>
                        val message = s"unexpected constant $instr"
                        throw BytecodeProcessingFailedException(message)
                }
            }

            @inline def compareValues(op: RelationalOperator): Unit = {
                val value2 = operandUse(0)
                val value1 = operandUse(1)
                val compare = Compare(pc, value1, op, value2)
                addInitLocalValStmt(pc, operandsArray(nextPC).head, compare)
            }

            @inline def ifCMPXXX(condition: RelationalOperator, branchoffset: Int): Unit = {
                val targetPC = pc + branchoffset
                val successors = domain.allSuccessorsOf(pc)
                if (successors.size == 1) {
                    val successorPC = successors.head
                    // HERE(!), the successor of the bb(!) can also be an ExitNode/CatchNode if
                    // the if falls through. In this case the block may end with, e.g., a return
                    // instruction, and therefore the successor is the ExitNode.
                    if (successorPC == nextPC || allDead(nextPC, successorPC)) {
                        // This "if" always either falls through or "jumps" to the next
                        // instruction ... => replace it by a NOP
                        addNOPAndKillOperandBasedUsages(2)
                    } else {
                        // This "if" is just a goto...
                        assert(targetPC != nextPC)
                        killOperandBasedUsages(pc, 2)
                        addStmt(Goto(pc, targetPC))
                    }
                } else {
                    val value2 = operandUse(0)
                    val value1 = operandUse(1)
                    addStmt(If(pc, value1, condition, value2, targetPC))
                }
            }

            @inline def ifXXX(
                condition:    RelationalOperator,
                branchoffset: Int,
                cmpVal:       => Expr[DUVar[aiResult.domain.DomainValue]]
            ): Unit = {
                val targetPC = pc + branchoffset
                val successors = domain.allSuccessorsOf(pc)
                if (successors.size == 1) {
                    /* ... in this case the if can be somewhere in a bb */
                    val successorPC = successors.head
                    // HERE(!), the successor of the bb(!) can also be an ExitNode/CatchNode if
                    // the if falls through. In this case the block may end with, e.g., a return
                    // instruction, and therefore the successor is the ExitNode.
                    if (successorPC == nextPC ||
                        /*jump over a bunch of dead code */ allDead(nextPC, successorPC)) {
                        // This "if" always either falls through or "jumps" to the next
                        // instruction ... => replace it by a NOP
                        addNOPAndKillOperandBasedUsages(1)
                    } else {
                        // This "if" is just a goto...
                        assert(targetPC != nextPC)
                        killOperandBasedUsages(pc, 1)
                        addStmt(Goto(pc, targetPC))
                    }
                } else {
                    val value = operandUse(0)
                    addStmt(If(pc, value, condition, cmpVal, targetPC))
                }
            }

            def as[T <: Instruction](i: Instruction): T = i.asInstanceOf[T]

            (opcode: @switch) match {
                case ALOAD_0.opcode | ALOAD_1.opcode | ALOAD_2.opcode | ALOAD_3.opcode |
                    ALOAD.opcode |
                    ASTORE_0.opcode | ASTORE_1.opcode | ASTORE_2.opcode | ASTORE_3.opcode |
                    ASTORE.opcode |
                    ILOAD_0.opcode | ILOAD_1.opcode | ILOAD_2.opcode | ILOAD_3.opcode |
                    ILOAD.opcode |
                    ISTORE_0.opcode | ISTORE_1.opcode | ISTORE_2.opcode | ISTORE_3.opcode |
                    ISTORE.opcode |
                    DLOAD_0.opcode | DLOAD_1.opcode | DLOAD_2.opcode | DLOAD_3.opcode |
                    DLOAD.opcode |
                    DSTORE_0.opcode | DSTORE_1.opcode | DSTORE_2.opcode | DSTORE_3.opcode |
                    DSTORE.opcode |
                    FLOAD_0.opcode | FLOAD_1.opcode | FLOAD_2.opcode | FLOAD_3.opcode |
                    FLOAD.opcode |
                    FSTORE_0.opcode | FSTORE_1.opcode | FSTORE_2.opcode | FSTORE_3.opcode |
                    FSTORE.opcode |
                    LLOAD_0.opcode | LLOAD_1.opcode | LLOAD_2.opcode | LLOAD_3.opcode |
                    LLOAD.opcode |
                    LSTORE_0.opcode | LSTORE_1.opcode | LSTORE_2.opcode | LSTORE_3.opcode |
                    LSTORE.opcode =>
                    addNOP(pc)

                case IRETURN.opcode | LRETURN.opcode | FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode =>
                    addStmt(ReturnValue(pc, operandUse(0)))

                case RETURN.opcode => addStmt(Return(pc))

                case AALOAD.opcode |
                    DALOAD.opcode | FALOAD.opcode | LALOAD.opcode |
                    IALOAD.opcode | SALOAD.opcode | CALOAD.opcode |
                    BALOAD.opcode => arrayLoad()

                case AASTORE.opcode | DASTORE.opcode |
                    FASTORE.opcode | IASTORE.opcode |
                    LASTORE.opcode | SASTORE.opcode |
                    BASTORE.opcode | CASTORE.opcode =>
                    val operandVar = operandUse(0)
                    val index = operandUse(1)
                    val arrayRef = operandUse(2)
                    addStmt(ArrayStore(pc, arrayRef, index, operandVar))

                case ARRAYLENGTH.opcode =>
                    val arrayRef = operandUse(0)
                    val lengthExpr = ArrayLength(pc, arrayRef)
                    if (wasExecuted(nextPC)) { // the next instruction cannot be a handler instruction
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, lengthExpr)
                    } else {
                        addStmt(ExprStmt(pc, lengthExpr))
                    }

                case BIPUSH.opcode | SIPUSH.opcode =>
                    val value = as[LoadConstantInstruction[Int]](instruction).value
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, IntConst(pc, value))

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode =>
                    val IFICMPInstruction(condition, branchoffset) = instruction
                    ifCMPXXX(condition, branchoffset)

                case IF_ACMPEQ.opcode | IF_ACMPNE.opcode =>
                    val IFACMPInstruction(condition, branchoffset) = instruction
                    ifCMPXXX(condition, branchoffset)

                case IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode =>
                    val IF0Instruction(condition, branchoffset) = instruction
                    ifXXX(condition, branchoffset, IntConst(ai.ConstantValueOrigin, 0))

                case IFNONNULL.opcode | IFNULL.opcode =>
                    val IFXNullInstruction(condition, branchoffset) = instruction
                    ifXXX(condition, branchoffset, NullExpr(ai.ConstantValueOrigin))

                case DCMPG.opcode | FCMPG.opcode => compareValues(CMPG)
                case DCMPL.opcode | FCMPL.opcode => compareValues(CMPL)
                case LCMP.opcode                 => compareValues(CMP)

                case SWAP.opcode                 => addNOP(pc)

                case DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode =>
                    binaryArithmeticOperation(Add)
                case DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode =>
                    binaryArithmeticOperation(Divide)
                case DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode =>
                    binaryArithmeticOperation(Multiply)
                case DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode =>
                    binaryArithmeticOperation(Modulo)
                case DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode =>
                    binaryArithmeticOperation(Subtract)

                case DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode =>
                    prefixArithmeticOperation(Negate)

                case IINC.opcode =>
                    val IINC(index, const) = instruction
                    val value = registerUse(index)
                    val incVal = IntConst(pc, const)
                    val iinc = BinaryExpr(pc, ComputationalTypeInt, Add, value, incVal)
                    addInitLocalValStmt(pc, localsArray(nextPC)(index), iinc)

                case IAND.opcode | LAND.opcode   => binaryArithmeticOperation(And)
                case IOR.opcode | LOR.opcode     => binaryArithmeticOperation(Or)
                case ISHL.opcode | LSHL.opcode   => binaryArithmeticOperation(ShiftLeft)
                case ISHR.opcode | LSHR.opcode   => binaryArithmeticOperation(ShiftRight)
                case IUSHR.opcode | LUSHR.opcode => binaryArithmeticOperation(UnsignedShiftRight)
                case IXOR.opcode | LXOR.opcode   => binaryArithmeticOperation(XOr)

                case ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode | ICONST_5.opcode |
                    ICONST_M1.opcode =>
                    val IConstInstruction(value) = instruction
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, IntConst(pc, value))

                case ACONST_NULL.opcode =>
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, NullExpr(pc))

                case DCONST_0.opcode | DCONST_1.opcode =>
                    val value = as[LoadConstantInstruction[Double]](instruction).value
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, DoubleConst(pc, value))

                case FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode =>
                    val value = as[LoadConstantInstruction[Float]](instruction).value
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, FloatConst(pc, value))

                case LCONST_0.opcode | LCONST_1.opcode =>
                    val value = as[LoadConstantInstruction[Long]](instruction).value
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, LongConst(pc, value))

                case LDC.opcode | LDC_W.opcode | LDC2_W.opcode =>
                    loadConstant(as[LoadConstantInstruction[_]](instruction))

                case INVOKEINTERFACE.opcode | INVOKESPECIAL.opcode | INVOKEVIRTUAL.opcode =>
                    val call @ MethodInvocationInstruction(
                        declClass, isInterface,
                        name, descriptor) = instruction
                    val parametersCount = descriptor.parametersCount
                    val params = useOperands(parametersCount).reverse
                    val receiver = operandUse(parametersCount) // this is the self reference
                    val returnType = descriptor.returnType
                    if (returnType.isVoidType) {
                        if (call.isVirtualMethodCall)
                            addStmt(VirtualMethodCall(
                                pc,
                                declClass, isInterface, name, descriptor,
                                receiver,
                                params
                            ))
                        else
                            addStmt(NonVirtualMethodCall(
                                pc,
                                declClass.asObjectType, isInterface, name, descriptor,
                                receiver,
                                params
                            ))
                    } else {
                        val expr =
                            if (call.isVirtualMethodCall)
                                VirtualFunctionCall(
                                    pc,
                                    declClass, isInterface, name, descriptor,
                                    receiver,
                                    params
                                )
                            else
                                NonVirtualFunctionCall(
                                    pc,
                                    declClass.asObjectType, isInterface, name, descriptor,
                                    receiver,
                                    params
                                )
                        if (wasExecuted(nextPC)) {
                            addInitLocalValStmt(pc, operandsArray(nextPC).head, expr)
                        } else {
                            addStmt(ExprStmt(pc, expr))
                        }
                    }

                case INVOKESTATIC.opcode =>
                    val INVOKESTATIC(declaringClass, isInterface, name, descriptor) = instruction
                    val parametersCount = descriptor.parametersCount
                    val params = useOperands(parametersCount).reverse
                    val returnType = descriptor.returnType
                    if (returnType.isVoidType) {
                        val staticCall =
                            StaticMethodCall(
                                pc,
                                declaringClass, isInterface, name, descriptor,
                                params
                            )
                        addStmt(staticCall)
                    } else {
                        val expr =
                            StaticFunctionCall(
                                pc,
                                declaringClass, isInterface, name, descriptor,
                                params
                            )
                        if (wasExecuted(nextPC)) {
                            addInitLocalValStmt(pc, operandsArray(nextPC).head, expr)
                        } else {
                            addStmt(ExprStmt(pc, expr))
                        }
                    }

                case INVOKEDYNAMIC.opcode =>
                    val INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = instruction
                    val parametersCount = descriptor.parametersCount
                    val params = useOperands(parametersCount).reverse
                    if (descriptor.returnType.isVoidType) {
                        val indyMethodCall =
                            InvokedynamicMethodCall(pc, bootstrapMethod, name, descriptor, params)
                        addStmt(indyMethodCall)
                    } else {
                        val indyFunctionCall =
                            InvokedynamicFunctionCall(pc, bootstrapMethod, name, descriptor, params)
                        if (wasExecuted(nextPC)) {
                            addInitLocalValStmt(pc, operandsArray(nextPC).head, indyFunctionCall)
                        } else {
                            addStmt(ExprStmt(pc, indyFunctionCall))
                        }
                    }

                case PUTSTATIC.opcode =>
                    val PUTSTATIC(declaringClass, name, fieldType) = instruction
                    val value = operandUse(0)
                    val putStatic = PutStatic(pc, declaringClass, name, fieldType, value)
                    addStmt(putStatic)

                case PUTFIELD.opcode =>
                    val PUTFIELD(declaringClass, name, fieldType) = instruction
                    val value = operandUse(0)
                    val objRef = operandUse(1)
                    val putField = PutField(pc, declaringClass, name, fieldType, objRef, value)
                    addStmt(putField)

                case GETSTATIC.opcode =>
                    val GETSTATIC(declaringClass, name, fieldType) = instruction
                    val getStatic = GetStatic(pc, declaringClass, name, fieldType)
                    // Given that we currently *not* model load-time exception/handling of
                    // corrupt/incompatible code bases, GETSTATIC will not throw an exception.
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, getStatic)

                case GETFIELD.opcode =>
                    val GETFIELD(declaringClass, name, fieldType) = instruction
                    val getField = GetField(pc, declaringClass, name, fieldType, operandUse(0))
                    if (wasExecuted(nextPC)) {
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, getField)
                    } else { // ... here: NullPointerException
                        addStmt(ExprStmt(pc, getField))
                    }

                case NEW.opcode =>
                    val NEW(objectType) = instruction
                    val newObject = New(pc, objectType)
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, newObject)

                case NEWARRAY.opcode =>
                    newArray(ArrayType(as[NEWARRAY](instruction).elementType))

                case ANEWARRAY.opcode =>
                    newArray(ArrayType(as[ANEWARRAY](instruction).componentType))

                case MULTIANEWARRAY.opcode =>
                    val MULTIANEWARRAY(arrayType, dimensions) = instruction
                    val counts = (0 until dimensions).map(d => operandUse(d))
                    val newArray = NewArray(pc, counts, arrayType)
                    if (wasExecuted(nextPC)) {
                        addInitLocalValStmt(pc, operandsArray(nextPC).head, newArray)
                    } else { // ... here: NegativeDimensionSize...
                        addStmt(ExprStmt(pc, newArray))
                    }

                case GOTO.opcode | GOTO_W.opcode =>
                    val GotoInstruction(branchoffset) = instruction
                    val targetPC = pc + branchoffset
                    if (targetPC == nextPC) {
                        // this goto "jumps" to the immediately succeeding instruction
                        addNOP(pc)
                    } else {
                        addStmt(Goto(pc, targetPC))
                    }

                case br.instructions.JSR.opcode | br.instructions.JSR_W.opcode =>
                    val JSRInstruction(branchoffset) = instruction
                    addStmt(JSR(pc, pc + branchoffset))
                case RET.opcode =>
                    addStmt(Ret(pc, cfg.successors(pc)))

                case NOP.opcode | POP.opcode | POP2.opcode => addNOP(pc)

                case INSTANCEOF.opcode =>
                    val value1 = operandUse(0)
                    val INSTANCEOF(tpe) = instruction
                    val instanceOf = InstanceOf(pc, value1, tpe)
                    addInitLocalValStmt(pc, operandsArray(nextPC).head, instanceOf)

                case CHECKCAST.opcode =>
                    val value1 = operandUse(0)
                    val CHECKCAST(targetType) = instruction
                    addStmt(Checkcast(pc, value1, targetType))

                case MONITORENTER.opcode => addStmt(MonitorEnter(pc, operandUse(0)))
                case MONITOREXIT.opcode  => addStmt(MonitorExit(pc, operandUse(0)))

                //
                //      H A N D L I N G   S W I T C H    S T A T E M E N T S
                //
                // It may be the case that some or all except of one branch are actually
                // dead – in particular in obfuscated code - in this cases, we have to
                // rewrite the switch statement. Given that at least one branch has to
                // be live, we can use that branches jump target as the jump target of the
                // goto instruction; in this case it doesn't matter whether it is a default
                // branch target or some other branch target. (The default can also be
                // dead if the switch is exhaustive; in that case, we can choose an arbitrary
                // value and consider its target as the default target.)
                // When we rewrite a switch to a goto, we have to kill the operand usage
                // and add a goto unless the goto simply jumps to the next instruction in
                // which case, we replace it by NOP.

                case TABLESWITCH.opcode =>
                    val index = operandUse(0)
                    val tableSwitch = as[TABLESWITCH](instruction)
                    val defaultTarget = pc + tableSwitch.defaultOffset
                    var caseValue = tableSwitch.low
                    val npairs = tableSwitch.jumpOffsets.map[IntIntPair] { jo =>
                        val caseTarget = pc + jo
                        val npair = IntIntPair(caseValue, caseTarget)
                        caseValue += 1
                        npair
                    }
                    addStmt(Switch(pc, defaultTarget, index, npairs))
                // FIXME  see general comment

                case LOOKUPSWITCH.opcode =>
                    val index = operandUse(0)
                    val lookupSwitch = as[LOOKUPSWITCH](instruction)
                    val defaultTarget = pc + lookupSwitch.defaultOffset
                    val npairs = lookupSwitch.npairs.map[IntIntPair] { npair =>
                        npair.incrementValue(increment = pc)
                    }
                    addStmt(Switch(pc, defaultTarget, index, npairs))
                // FIXME  see general comment

                case DUP.opcode | DUP_X1.opcode | DUP_X2.opcode
                    | DUP2.opcode | DUP2_X1.opcode | DUP2_X2.opcode => addNOP(pc)

                case D2F.opcode | I2F.opcode | L2F.opcode => primitiveCastOperation(FloatType)
                case D2I.opcode | F2I.opcode | L2I.opcode => primitiveCastOperation(IntegerType)
                case D2L.opcode | I2L.opcode | F2L.opcode => primitiveCastOperation(LongType)
                case F2D.opcode | I2D.opcode | L2D.opcode => primitiveCastOperation(DoubleType)
                case I2C.opcode                           => primitiveCastOperation(CharType)
                case I2B.opcode                           => primitiveCastOperation(ByteType)
                case I2S.opcode                           => primitiveCastOperation(ShortType)

                case ATHROW.opcode                        => addStmt(Throw(pc, operandUse(0)))

                case WIDE.opcode                          => addNOP(pc)

                case opcode =>
                    throw BytecodeProcessingFailedException(s"unknown opcode: $opcode")
            }

            pc = nextPC
            while (pc < codeSize && !wasExecuted(pc)) {
                pcToIndex(pc) = -1
                pc = pcOfNextInstruction(pc)
            }
        } while (pc < codeSize)

        // add the artificial lastPC + 1 instruction to enable the mapping of exception handlers
        pcToIndex(pc /* == codeSize +1 */ ) = index

        val tacParams: Parameters[TACMethodParameter] = {
            import descriptor.parameterTypes
            if (descriptor.parametersCount == 0 && isStatic)
                NoParameters.asInstanceOf[Parameters[TACMethodParameter]]
            else {
                val paramCount = descriptor.parametersCount + 1
                val paramDVars = new Array[TACMethodParameter](paramCount)

                var defOrigin = -1
                if (!method.isStatic) {
                    var usedBy = domain.usedBy(-1)
                    if (usedBy eq null) {
                        usedBy = IntTrieSet.empty
                    } else {
                        usedBy = usedBy.map(pcToIndex)
                    }
                    paramDVars(0) = new TACMethodParameter(-1, usedBy)
                    defOrigin = -2
                }
                var pIndex = 1
                while (pIndex < paramCount) {
                    var usedBy = domain.usedBy(defOrigin)
                    // the usedBy for parameters never refer to parameters => have negative values!
                    if (usedBy eq null) {
                        usedBy = IntTrieSet.empty
                    } else {
                        usedBy = usedBy.map(pcToIndex)
                    }
                    paramDVars(pIndex) = new TACMethodParameter(-pIndex - 1, usedBy)
                    defOrigin -= parameterTypes(pIndex - 1).operandSize
                    pIndex += 1
                }
                new Parameters(paramDVars)
            }
        }

        // INVARIANTS:
        //  - every pc appears at most once in `obsoleteUseSites`
        //  - we do not have deeply nested expressions
        while (obsoleteUseSites.nonEmpty) {
            val /*original - bytecode based...:*/ useDefMapping = obsoleteUseSites.removeHead()
            val useSite = useDefMapping.pc
            val defSites = useDefMapping.value
            // Now... we need to go the def site - which has to be an assignment - and kill
            // the respective use unless it is an exception...; if no use remains...
            //      and the expression is side effect free ... add it to obsoleteDefSites and
            //                                                 replace it by a nop
            //      and the expression has a side effect ... replace the Assignment by an ExprStmt
            defSites foreach { defSite =>
                if (defSite >= 0) {
                    val defSiteIndex = pcToIndex(defSite)
                    val Assignment(pc, v @ DVar(_, useSites), expr) = statements(defSiteIndex)
                    val newUseSites = useSites - useSite
                    if (newUseSites.nonEmpty) {
                        val newAssignment = Assignment(pc, v.copy(useSites = newUseSites), expr)
                        statements(defSiteIndex) = newAssignment
                    } else if (expr.isSideEffectFree) {
                        val instruction = instructions(defSite)
                        instruction match {
                            case IINC(lvIndex, _) =>
                                killRegisterBasedUsages(defSite, lvIndex)
                                statements(defSiteIndex) = Nop(pc)
                            case _ =>
                                // we have to kill as many "uses" as the original - underlying -
                                // bytecode instruction to handle constant propagation
                                killOperandBasedUsages(defSite, expr.subExprCount)
                                statements(defSiteIndex) = Nop(pc)
                        }
                    } else {
                        statements(defSiteIndex) = ExprStmt(pc, expr)
                    }
                } else if (ImmediateVMExceptionsOriginOffset < defSite /*&& < 0*/ ) {
                    // We have an obsolete parameter usage; recall that the def-sites are
                    // already "normalized"!
                    val TACMethodParameter(origin, useSites) = tacParams.parameter(defSite)
                    // Note that the "use sites" of the parameters are already remapped.
                    val newUseSites = useSites - pcToIndex(useSite)
                    tacParams.parameters(-defSite - 1) = new TACMethodParameter(origin, newUseSites)
                } else {
                    /* IMPROVE Support tracking def->use information for exceptions (currently we only have use->def.)
                    val useSiteIndex = pcToIndex(useSite)
                    val defSitePC = ai.pcOfVMLevelValue(defSite)
                    // we have an obsolete exception usage; see
                    //      ai.MethodsWithexceptoins.nestedTryFinally()
                    // for an example which has dead code that leads to an obsolete exception usage
                    */
                    ;
                }
            }
        }
        type ValueInformation = aiResult.domain.DomainValue
        type AIDUVar = DUVar[ValueInformation]
        val tacStmts: Array[Stmt[AIDUVar]] = {
            def isIndexOfCaughtExceptionStmt(index: Int): Boolean = {
                statements(index).astID == CaughtException.ASTID
            }
            if (index == maxStatements) {
                var s = 0
                while (s < maxStatements) {
                    statements(s).remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
                    s += 1
                }
                statements
            } else {
                val tacStmts = new Array[Stmt[AIDUVar]](index)
                var s = 0
                while (s < index) {
                    val stmt = statements(s)
                    stmt.remapIndexes(pcToIndex, isIndexOfCaughtExceptionStmt)
                    tacStmts(s) = stmt
                    s += 1
                }
                tacStmts
            }
        }
        def singletonBBsExpander(index: Int): Int = {
            // The set addedHandlerStmts may contain handler pcs not related to the singleton bbs,
            // but this doesn't matter, because this function as a whole is only called for
            // singleton bbs.
            if (addedHandlerStmts.contains(index)) index + 1 else index
        }
        val tacCFG =
            cfg.mapPCsToIndexes[Stmt[AIDUVar], TACStmts[AIDUVar]](
                TACStmts(tacStmts),
                pcToIndex,
                singletonBBsExpander,
                lastIndex = index - 1
            )
        val tacExceptionHandlers = updateExceptionHandlers(pcToIndex)(aiResult)
        val initialTAC = new AITACode[TACMethodParameter, ValueInformation](
            tacParams, tacStmts, pcToIndex, tacCFG, tacExceptionHandlers
        )

        // Potential Optimizations
        // =======================
        // (1) Optimizing "not operations on booleans"... e.g., Java code such as m(!a) which is
        // compiled to:
        //      if(a)
        //          val b = false
        //      else
        //          val c = true
        //      m({b,c})
        // Goal:
        //      val c = !a
        //      m(c)
        //
        // (2) Perform constant propagation when a use-site has only a _single_ def-site:
        //     val x = 304
        //     if({a,b} != x) goto t
        //          =>  afterwards check if the def site has more use sites and – if not –
        //              replace it by nops
        //
        //
        // (3) Identify _really_ useless nops and remove them. (Note that some nops may be
        //     required to ensure that the path information is available. E.g.,
        //     java.lang.String.<init>(byte[],int,int,int) (java 1.8.0_181)

        // Non-Optimizations
        // =================
        // The following code looks optimizable, but is actually not optimizable!
        //
        // Seemingly useless if-gotos; e.g., (taken from "java.lang.String replace(char,char)")
        // 20: if({a} != {param1}) goto 22
        //       // 20 ->
        // 21:/*pc=102:*/ goto 23
        //       // 20 ->
        // 22:/*pc=105:*/ ;
        //       // 21, 22 ->
        // 23:/*pc=107:*/ {lvb}[{lv18, lv5}] = {param2, lv13}
        //       // ⚡️ <uncaught exception => abnormal return>
        // THIS CODE CANNOT BE OPTIMIZED BECAUSE THE CONTROL-FLOW IS RELEVANT TO DETERMINE
        // WHICH VALUES (param2 or lv13 / lv18 or lv5) ARE ACTUALLY SELECTED AT RUNTIME.
        // HOWEVER, THIS IS NOT DIRECTLY OBVIOUS FROM THE ABOVE CODE!

        if (optimizations.nonEmpty) {
            val base = TACOptimizationResult[TACMethodParameter, DUVar[aiResult.domain.DomainValue], AITACode[TACMethodParameter, aiResult.domain.DomainValue]](initialTAC, wasTransformed = false)
            val result = optimizations.foldLeft(base)((tac, optimization) => optimization(tac))
            result.code
        } else {
            initialTAC
        }
    }

}
