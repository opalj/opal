/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ll.fpcf.analyses.ifds.taint

import org.opalj.br.analyses.SomeProject
import org.opalj.ll.fpcf.analyses.ifds.{LLVMStatement, NativeBackwardIFDSProblem, NativeFunction}
import org.opalj.ll.llvm.value._
import org.opalj.tac.fpcf.analyses.ifds.taint.{TaintFact, TaintProblem}

abstract class NativeBackwardTaintProblem(project: SomeProject)
    extends NativeBackwardIFDSProblem[NativeTaintFact, TaintFact](project)
        with TaintProblem[NativeFunction, LLVMStatement, NativeTaintFact] {

    override def nullFact: NativeTaintFact = NativeTaintNullFact

    override def normalFlow(statement: LLVMStatement, in: NativeTaintFact,
                            predecessor: Option[LLVMStatement]): Set[NativeTaintFact] = statement.instruction match {
        case store: Store => in match {
            case NativeVariable(value) if value == store.dst => Set(in, NativeVariable(store.src))
            case NativeArrayElement(base, indices) => store.dst match { // dst is pointer type
                // value is array and is stored to tainted alloca
                case dst: Alloca if dst == base => Set(in, NativeArrayElement(store.src, indices))
                // value is stored into tainted array element
                case gep: GetElementPtr if gep.base == base =>
                    // if indices are not constant, assume the tainted element is written
                    if ((gep.isConstant && gep.constants.exists(indices.toSeq.contains(_))) || !gep.isConstant)
                        Set(in, NativeVariable(store.src))
                    else Set(in)
            }
            case _ => Set(in)
        }
        case load: Load => in match {
            case NativeVariable(value) if value == load => Set(in, NativeVariable(load.src))
            case NativeArrayElement(base, indices) if base == load => load.src match { // src is pointer type
                // array loaded from alloca
                case src: Alloca => Set(in, NativeArrayElement(src, indices))
                // array loaded from array element -> nested arrays
                case gep: GetElementPtr =>
                    if (gep.isConstant) Set(in, NativeArrayElement(gep.base, gep.constants))
                    else Set(in, NativeVariable(gep.base)) // taint whole array if indices are not constant
                case _ => Set(in)
            }
            case _ => Set(in)
        }
        case gep: GetElementPtr => in match {
            case NativeVariable(value) if value == gep => Set(in, NativeVariable(gep.base))
            case NativeArrayElement(base, indices) if base == gep && gep.isZero => Set(in, NativeArrayElement(gep.base, indices))
            case _ => Set(in)
        }
        case fneg: FNeg => in match {
            case NativeVariable(value) if value == fneg => Set(in, NativeVariable(fneg.operand(0)))
            case _ => Set(in)
        }
        case binOp: BinaryOperation => in match {
            case NativeVariable(value) if value == binOp => Set(in, NativeVariable(binOp.op1), NativeVariable(binOp.op2))
            case _ => Set(in)
        }
        case convOp: ConversionOperation => in match {
            case NativeVariable(value) if value == convOp => Set(in, NativeVariable(convOp.value))
            case _ => Set(in)
        }
        case extrElem: ExtractElement =>
            def taintExtElemVec: Set[NativeTaintFact] = {
                if (extrElem.isConstant) Set(in, NativeArrayElement(extrElem.vec, Seq(extrElem.constant)))
                else Set(in, NativeVariable(extrElem.vec)) // taint whole array if index not constant
            }
            in match {
                case NativeVariable(value) if value == extrElem => taintExtElemVec
                case NativeArrayElement(base, _) if base == extrElem => taintExtElemVec
                case _ => Set(in)
            }
        case insElem: InsertElement => in match {
            case NativeVariable(value) if insElem.vec == value => Set(in, NativeVariable(insElem.value))
            case NativeArrayElement(base, indices) if insElem.vec == base =>
                // check if tainted element is written. if index is not constant, assume tainted element is written.
                if ((insElem.isConstant && indices.exists(_ == insElem.constant)) || !insElem.isConstant)
                    Set(in, NativeVariable(insElem.value))
                else Set(in)
            case _ => Set(in)
        }
        case shuffleVec: ShuffleVector => in match {
            // simplification: taint both input arrays as a whole
            case NativeVariable(value) if value == shuffleVec =>
                Set(in, NativeVariable(shuffleVec.vec1), NativeVariable(shuffleVec.vec2))
            case NativeArrayElement(base, _) if base == shuffleVec =>
                Set(in, NativeVariable(shuffleVec.vec1), NativeVariable(shuffleVec.vec2))
            case _ => Set(in)
        }
        case extrValue: ExtractValue => in match {
            case NativeVariable(value) if extrValue == value =>
                Set(in, NativeArrayElement(extrValue.aggregVal, extrValue.constants))
            case NativeArrayElement(base, _) if base == extrValue =>
                // array loaded from array element -> nested arrays
                Set(in, NativeArrayElement(extrValue.aggregVal, extrValue.constants))
            case _ => Set(in)
        }
        case insValue: InsertValue => in match {
            case NativeVariable(value) if insValue.aggregVal == value => Set(in, NativeVariable(insValue.value))
            // check if tainted element is written
            case NativeArrayElement(base, indices) if insValue.aggregVal == base &&
                insValue.constants.exists(indices.toSeq.contains(_)) => Set(in, NativeVariable(insValue.value))
            case _ => Set(in)
        }
        case _ => Set(in)
    }

}
