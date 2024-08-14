package org.opalj.tactobc

import org.opalj.br.Method
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{ArrayLength, ArrayLoad, ArrayStore, Assignment, BinaryExpr, Checkcast, Compare, DUVar, Expr, ExprStmt, GetField, If, InvokedynamicFunctionCall, NewArray, NonVirtualMethodCall, PrefixExpr, PrimitiveTypecastExpr, PutField, PutStatic, ReturnValue, StaticFunctionCall, StaticMethodCall, Stmt, Switch, Throw, UVar, VirtualFunctionCall, VirtualMethodCall}
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
  def prepareLVIndexes(method: Method, tacStmts: Array[(Stmt[DUVar[ValueInformation]], Int)]): Unit = {
    // container for all DUVars in the method
    val isStaticMethod = method.isStatic
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
        case Throw(_, exception) =>
          collectDUVarFromExpr(exception, duVars)
        case Switch(_, _, index, _) =>
          collectDUVarFromExpr(index, duVars)
        case Checkcast(_, value, _) =>
          collectDUVarFromExpr(value, duVars)
        case _ =>
      }
      }
    }
    // give the first available indexes to parameters
    val parameters = mapParametersAndPopulate(duVars, isStaticMethod)
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
  def mapParametersAndPopulate(duVars: mutable.ListBuffer[DUVar[_]], isStaticMethod: Boolean): mutable.Map[IntTrieSet, Int] = {
    // Step 2: Filter and Sort DUVar instances with negative origins
    val parameterVars = duVars.collect {
      case uVar: UVar[_] if uVar.defSites.exists(_ < 0) => uVar
    }
    val seenDefSites = mutable.Set[Int]()
    val uniqueParameterVars = parameterVars.filter { uVar =>
      // Extract the minimum defSite from the IntTrieSet
      val minDefSite = uVar.defSites.head

      // Check if we've already seen this defSite
      if (seenDefSites.contains(minDefSite)) {
        false // Skip this UVar, as it's already been added
      } else {
        seenDefSites += minDefSite // Mark this defSite as seen
        true // Keep this UVar
      }
    }
    val sortedParameterVars = uniqueParameterVars.sortBy { uVar =>
      // Sort by the minimum value in defSites, since we want the most negative number first
      uVar.defSites.head
    }(Ordering[Int].reverse)
    println(sortedParameterVars)
    // Iterate over the sorted list of unique parameters
    sortedParameterVars.foreach { uVar =>
      // Check if the defSites contain a parameter origin
      val existingEntry = uVarToLVIndex.find { case (key, _) => key.intersect(uVar.defSites).nonEmpty }
      existingEntry match {
        case Some((existingDefSites, _)) => // Do nothing if already processed
        case None =>
          uVar.defSites.foreach { origin =>
            if (origin == -1 && !isStaticMethod) {
              // Assign LV index 0 for 'this' for instance methods
              uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), 0)
            } else if (origin == -2) {
              nextLVIndex = if (isStaticMethod) 0 else 1 // Start at 1 for instance methods to reserve 0 for 'this'
              uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), nextLVIndex)
              incrementLVIndex(uVar)
            } else if (origin < -2) {
              uVarToLVIndex.getOrElseUpdate(IntTrieSet(origin), nextLVIndex)
              incrementLVIndex(uVar)
            }
          }
      }
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
          incrementLVIndex(uVar)
          lvIndex
        })
    }
  }

  /**
   * Increments the LV index appropriately based on the type of the UVar.
   *
   * @param uVar The UVar for which the LV index is to be incremented.
   */
  def incrementLVIndex(uVar: UVar[_]): Unit = {
    // Temporary type checking using toString method
    val isDoubleOrLongType = uVar.value.toString.contains("long") || uVar.value.toString.contains("Long")|| uVar.value.toString.contains("Double")
    nextLVIndex += (if (isDoubleOrLongType) 2 else 1)
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
      case invokedynamicFunctionCall: InvokedynamicFunctionCall[_] => collectDUVarFromInvokedynamicFunctionCall(invokedynamicFunctionCall, duVars)
      case getField: GetField[_] =>  collectDUVarFromGetField(getField, duVars)
      case compare: Compare[_] => collectDUVarFromCompare(compare, duVars)
      case prefixExpr: PrefixExpr[_] => collectDUVarFromPrefixExpr(prefixExpr, duVars)
      case _ =>
    }
  }

  def collectDUVarFromPrefixExpr(prefixExpr: PrefixExpr[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(prefixExpr.operand, duVars)
  }

  def collectDUVarFromCompare(compare: Compare[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(compare.left, duVars)
    collectDUVarFromExpr(compare.right, duVars)
  }

  def collectDUVarFromGetField(getField: GetField[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    collectDUVarFromExpr(getField.objRef, duVars)
  }

  def collectDUVarFromInvokedynamicFunctionCall(invokedynamicFunctionCall: InvokedynamicFunctionCall[_], duVars: mutable.ListBuffer[DUVar[_]]): Unit = {
    // Process each parameter and collect from each
    for (param <- invokedynamicFunctionCall.params) {
      collectDUVarFromExpr(param, duVars)
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
