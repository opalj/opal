package org.opalj.tactobc

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{Assignment, BinaryExpr, DUVar, Expr, If, NonVirtualMethodCall, PrimitiveTypecastExpr, PutField, ReturnValue, StaticFunctionCall, Stmt, UVar, VirtualFunctionCall, VirtualMethodCall}
import org.opalj.tactobc.ExprProcessor.{nextLVIndex, uVarToLVIndex}
import org.opalj.value.ValueInformation

import scala.collection.mutable

object FirstPass {

  /**
   * Collects all DUVars of the current method, adds parameters (if given) in the first available LVIndexes,
   * populates then the uVarToLVIndex map so that each used (and unique) variable does have a unique LVIndex.
   * @param stmt translateSingleTACtoBC iterates through all exisiting stmts for the method to be translated.
   *             The parameter "stmt" represents the current stmt to be inspected
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def prepareLVIndexes(tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    tacStmts.foreach { case (stmt, _) => {
      stmt match {
        case Assignment(_, targetVar, expr) =>
          collectDUVarFromExpr(targetVar, duVars)
          collectDUVarFromExpr(expr, duVars)
        case If(_, left, _, right, _) =>
          collectDUVarFromExpr(left, duVars)
          collectDUVarFromExpr(right, duVars)
        case VirtualMethodCall(_, _, _, _, _, receiver, params) =>
          collectDUVarFromExpr(receiver, duVars)
          for (param <- params) {
            collectDUVarFromExpr(param, duVars)
          }
        case PutField(_, _, _, _, objRef, value) =>
          collectDUVarFromExpr(objRef, duVars)
          collectDUVarFromExpr(value, duVars)
        case NonVirtualMethodCall(_, _, _, _, _, receiver, params) =>
          collectDUVarFromExpr(receiver, duVars)
          for (param <- params) {
            collectDUVarFromExpr(param, duVars)
          }
        case ReturnValue(_, expr) =>
          collectDUVarFromExpr(expr, duVars)
        case _ =>
      }
      }
    }
    // give the first available indexes to parameters
    val parameters = mapParametersAndPopulate(duVars)
    println(parameters)
    val lvIndexMap = collectAllUVarsAndPopulateUVarToLVIndexMap(duVars)
    println(lvIndexMap)
  }

  /**
   * Filters all UVars of the duVars map in the parameter and populates the uVarToLVIndex map in order so that
   * each unique uVar has a unique LVIndex
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   * @return a mutable.Map[IntTrieSet, Int]: the IntTrieSet corresponds a UVars defsites, the Int represents the LVIndex
   */
  def collectAllUVarsAndPopulateUVarToLVIndexMap(duVars: mutable.ListBuffer[DUVar[_]]): mutable.Map[IntTrieSet, Int] = {
    duVars.toArray.foreach {
      case uVar : UVar[_] => populateUvarToLVIndexMap(uVar)
      case _ =>
    }
    uVarToLVIndex
  }

  /**
   * Gives the first available LVIndexes to the parameters of the method
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   * @return a mutable.Map[IntTrieSet, Int]: the IntTrieSet corresponds a UVars defsites, the Int represents the LVIndex
   */
  def mapParametersAndPopulate(duVars: mutable.ListBuffer[DUVar[_]]): mutable.Map[IntTrieSet, Int] = {
    duVars.foreach {
      case uVar: UVar[_] if uVar.defSites.exists(origin => origin < 0) =>
        // Check if the defSites contain a parameter origin
        uVar.defSites.foreach { origin =>
          if (origin == -1) {
            // Assign LV index 0 for 'this' only for instance methods
            uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), 0)
          } else if (origin < -1) {
            if (origin == -2) {
              // Assign LV index 0 for 'this' only for instance methods
              uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), 0)
            } else {
              // Assign LV indexes for parameters
              uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), {
                val lvIndex = nextLVIndex
                nextLVIndex += 1
                lvIndex
              })
            }
          }
        }
      case _ =>
    }
    uVarToLVIndex
  }

  /**
   * Populates the uVarToLVIndex map by givin each unique uVar a unique LVIndex
   * @param uVar an used variable
   */
  def populateUvarToLVIndexMap(uVar: UVar[_]): Unit = {
    // Check if any existing key contains any of the def-sites
    val existingEntry = uVarToLVIndex.find { case (key, _) => key.intersect(uVar.defSites).nonEmpty }
    existingEntry match {
      case Some((existingDefSites, index)) =>
        // Merge the def-sites and update the map
        val mergedDefSites = existingDefSites ++ uVar.defSites
        uVarToLVIndex -= existingDefSites
        uVarToLVIndex(mergedDefSites) = index
      case None =>
        // No overlapping def-sites found, add a new entry
        uVarToLVIndex.getOrElseUpdate(uVar.defSites, {
          val lvIndex = nextLVIndex
          nextLVIndex += 1
          lvIndex
        })
    }
  }

  /**
   * Helper method to traverse over expressions and collect the DUVars "embedded" in them
   * @param expr the expression to be traversed
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def collectDUVarFromExpr(expr: Expr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    expr match {
      case duVar: DUVar[_] => duVars += duVar
      case binaryExpr: BinaryExpr[_] => collectDUVarFromBinaryExpr(binaryExpr, duVars)
      case virtualFunctionCallExpr: VirtualFunctionCall[_] => collectDUVarFromVirtualMethodCall(virtualFunctionCallExpr, duVars)
      case staticFunctionCallExpr: StaticFunctionCall[_] => collectDUVarFromStaticFunctionCall(staticFunctionCallExpr, duVars)
      case primitiveTypecaseExpr: PrimitiveTypecastExpr[_] => collectDUVarFromPrimitiveTypeCastExpr(primitiveTypecaseExpr, duVars)
      case _ =>
    }
  }

  /**
   * Helper method to traverse through a PrimitiveTypeCastExpr and collect the DUVars
   * @param primitiveTypecaseExpr an Expr of type PrimitiveTypecastExpr
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def collectDUVarFromPrimitiveTypeCastExpr(primitiveTypecaseExpr: PrimitiveTypecastExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(primitiveTypecaseExpr.operand, duVars)
  }

  /**
   * Helper method to traverse through a StaticFunctionCall Expr and collect the DUVars
   * @param staticFunctionCallExpr an Expr of type StaticFunctionCall
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def collectDUVarFromStaticFunctionCall(staticFunctionCallExpr: StaticFunctionCall[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    // Process each parameter and collect from each
    for (param <- staticFunctionCallExpr.params) {
      collectDUVarFromExpr(param, duVars)
    }
  }

  /**
   * Helper method to traverse through a BinaryExpr Expr and collect the DUVars
   * @param binaryExpr an Expr of type BinaryExpr
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def collectDUVarFromBinaryExpr(binaryExpr: BinaryExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(binaryExpr.left, duVars)
    collectDUVarFromExpr(binaryExpr.right, duVars)
  }

  /**
   * Helper method to traverse through a VirtualFunctionCall Expr and collect the DUVars
   * @param virtualFunctionCallExpr an Expr of type VirtualFunctionCall
   * @param duVars a ListBuffer[DUVar[_]] that is extended with all DUVars of the given method
   */
  def collectDUVarFromVirtualMethodCall(virtualFunctionCallExpr: VirtualFunctionCall[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(virtualFunctionCallExpr.receiver, duVars)
    for (param <- virtualFunctionCallExpr.params) {
      collectDUVarFromExpr(param, duVars)
    }
  }
}
