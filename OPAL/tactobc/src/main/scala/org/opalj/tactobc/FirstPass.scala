package org.opalj.tactobc

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{ArrayLength, ArrayLoad, ArrayStore, Assignment, BinaryExpr, DUVar, Expr, ExprStmt, If, NewArray, NonVirtualMethodCall, PrimitiveTypecastExpr, PutField, PutStatic, ReturnValue, StaticFunctionCall, StaticMethodCall, Stmt, UVar, VirtualFunctionCall, VirtualMethodCall}
import org.opalj.tactobc.ExprProcessor.{nextLVIndex, uVarToLVIndex}
import org.opalj.value.ValueInformation

import scala.collection.mutable

/**
 * Handles the initial preparation of local variable (LV) indexes
 * for translating three-address code (TAC) to bytecode. This involves collecting all
 * defined-use variables (DUVars) in the method, assigning LV indexes to parameters,
 * and populating a map that assigns unique LV indexes to each unique variable.
 *
 * Key responsibilities:
 * - Collect all DUVars from the method's statements.
 * - Assign LV indexes to method parameters.
 * - Populate a map (`uVarToLVIndex`) with unique LV indexes for each variable used in the method.
 */
object FirstPass {

  /**
   * Prepares local variable (LV) indexes for the given method by:
   * 1. Collecting all defined-use variables (DUVars) from the method's statements.
   * 2. Assigning LV indexes to method parameters.
   * 3. Populating the `uVarToLVIndex` map with unique LV indexes for each unique variable.
   *
   * @param tacStmts Array of tuples where each tuple contains a TAC statement and its index.
   */
  def prepareLVIndexes(tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)]): Unit = {
    // container for all DUVars in the method
    val duVars = mutable.ListBuffer[DUVar[_]]()
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
        case PutStatic(_, _, _, _, value) =>
          collectDUVarFromExpr(value, duVars)
        case StaticMethodCall(_,_,_,_,_,params) =>
          for (param <- params) {
            collectDUVarFromExpr(param, duVars)
          }
        case NonVirtualMethodCall(_, _, _, _, _, receiver, params) =>
          collectDUVarFromExpr(receiver, duVars)
          for (param <- params) {
            collectDUVarFromExpr(param, duVars)
          }
        case ReturnValue(_, expr) =>
          collectDUVarFromExpr(expr, duVars)
        case ArrayStore(_, arrayRef, index, value) =>
          collectDUVarFromExpr(arrayRef, duVars)
          collectDUVarFromExpr(index, duVars)
          collectDUVarFromExpr(value, duVars)
        case ExprStmt(_, expr) =>
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
   * Populates the `uVarToLVIndex` map with unique LV indexes for each variable in the given DUVars list.
   *
   * @param duVars ListBuffer containing all DUVars of the method.
   * @return A map where keys are def-sites of UVars and values are their corresponding LV indexes.
   */
  def collectAllUVarsAndPopulateUVarToLVIndexMap(duVars: mutable.ListBuffer[DUVar[_]]): mutable.Map[IntTrieSet, Int] = {
    duVars.toArray.foreach {
      case uVar : UVar[_] => populateUvarToLVIndexMap(uVar)
      case _ =>
    }
    uVarToLVIndex
  }

  /**
   * Assigns the first available LV indexes to method parameters.
   *
   * @param duVars ListBuffer containing all DUVars of the method.
   * @return A map where keys are def-sites of UVars and values are their corresponding LV indexes.
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
   * Populates the `uVarToLVIndex` map with unique LV indexes for each unique UVar.
   *
   * @param uVar A variable used in the method.
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
   * Traverses an expression to collect all DUVars embedded within it.
   *
   * @param expr The expression to be traversed
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromExpr(expr: Expr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    expr match {
      case duVar: DUVar[_] => duVars += duVar
      case binaryExpr: BinaryExpr[_] => collectDUVarFromBinaryExpr(binaryExpr, duVars)
      case virtualFunctionCallExpr: VirtualFunctionCall[_] => collectDUVarFromVirtualMethodCall(virtualFunctionCallExpr, duVars)
      case staticFunctionCallExpr: StaticFunctionCall[_] => collectDUVarFromStaticFunctionCall(staticFunctionCallExpr, duVars)
      case primitiveTypecaseExpr: PrimitiveTypecastExpr[_] => collectDUVarFromPrimitiveTypeCastExpr(primitiveTypecaseExpr, duVars)
      case arrayLengthExpr: ArrayLength[_] => collectDUVarFromArrayLengthExpr(arrayLengthExpr, duVars)
      case arrayLoadExpr: ArrayLoad[_] => collectDUVarFromArrayLoadExpr(arrayLoadExpr, duVars)
      case newArrayExpr: NewArray[_] => collectDUVarFromNewArrayExpr(newArrayExpr, duVars)
      case _ =>
    }
  }

  def collectDUVarFromNewArrayExpr(newArrayExpr: NewArray[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    for (count <- newArrayExpr.counts) {
      collectDUVarFromExpr(count, duVars)
    }
    // tpe does not contain any expr
  }

  /**
   * Traverses a `ArrayLoad` expr to collect all DUVars embedded within it.
   *
   * @param arrayLoadExpr The `ArrayLength` expr to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromArrayLoadExpr(arrayLoadExpr: ArrayLoad[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(arrayLoadExpr.index, duVars)
    collectDUVarFromExpr(arrayLoadExpr.arrayRef, duVars)
  }

  /**
   * Traverses a `ArrayLength` expr to collect all DUVars embedded within it.
   *
   * @param arrayLength The `ArrayLength` expr to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromArrayLengthExpr(arrayLength: ArrayLength[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(arrayLength.arrayRef, duVars)
  }

  /**
   * Traverses a `PrimitiveTypeCastExpr` to collect all DUVars embedded within it.
   *
   * @param primitiveTypecaseExpr The `PrimitiveTypecastExpr` to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromPrimitiveTypeCastExpr(primitiveTypecaseExpr: PrimitiveTypecastExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(primitiveTypecaseExpr.operand, duVars)
  }

  /**
   * Traverses a `StaticFunctionCall` expression to collect all DUVars embedded within it.
   *
   * @param staticFunctionCallExpr The `StaticFunctionCall` expression to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromStaticFunctionCall(staticFunctionCallExpr: StaticFunctionCall[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    // Process each parameter and collect from each
    for (param <- staticFunctionCallExpr.params) {
      collectDUVarFromExpr(param, duVars)
    }
  }

  /**
   * Traverses a `BinaryExpr` to collect all DUVars embedded within it.
   *
   * @param binaryExpr The `BinaryExpr` to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromBinaryExpr(binaryExpr: BinaryExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(binaryExpr.left, duVars)
    collectDUVarFromExpr(binaryExpr.right, duVars)
  }

  /**
   * Traverses a `VirtualFunctionCall` expression to collect all DUVars embedded within it.
   *
   * @param virtualFunctionCallExpr The `VirtualFunctionCall` expression to be traversed.
   * @param duVars ListBuffer to be extended with all DUVars found in the expression.
   */
  def collectDUVarFromVirtualMethodCall(virtualFunctionCallExpr: VirtualFunctionCall[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(virtualFunctionCallExpr.receiver, duVars)
    for (param <- virtualFunctionCallExpr.params) {
      collectDUVarFromExpr(param, duVars)
    }
  }
}
