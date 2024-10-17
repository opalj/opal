/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import org.opalj.br.{ClassFile, ObjectType}

import scala.collection.mutable.ListBuffer


/**
 * This class is used to reconstruct intent filters from the analysed project. It holds all relevant information to
 * simulate intent matching.
 *
 * @author Tom Nikisch
 */
class IntentFilter(var receiver: ClassFile, var componentType: String) {


    final val addDataAuthority = "addDataAuthority"
    final val addDataPath = "addDataPath"
    final val addDataScheme = "addDataScheme"
    final val addDataSSP = "addDataSchemeSpecificPart"
    final val addDataType = "addDataType"
    final val addAction = "addAction"
    final val addCategory = "addCategory"
    final val registerReceiver = "registerReceiver"

    final val filterDataMethods: List[String] = List(
        addDataAuthority, addDataPath, addDataScheme, addDataSSP, addDataType
    )

    val registeredReceivers: ListBuffer[ObjectType] = ListBuffer.empty[ObjectType]

    var actions: ListBuffer[String] = ListBuffer.empty[String]
    var categories: ListBuffer[String] = ListBuffer.empty[String]

    var dataTypes: ListBuffer[String] = ListBuffer.empty[String]
    var dataAuthorities: ListBuffer[String] = ListBuffer.empty[String]
    var dataPaths: ListBuffer[String] = ListBuffer.empty[String]
    var dataSchemes: ListBuffer[String] = ListBuffer.empty[String]
    var dataSSPs: ListBuffer[String] = ListBuffer.empty[String]

    def cloneFilter(): IntentFilter = {
        val clone = new IntentFilter(receiver, componentType)
        clone.actions = actions
        clone.categories = categories
        clone.dataAuthorities = dataAuthorities
        clone.dataTypes = dataTypes
        clone.dataPaths = dataPaths
        clone.dataSchemes = dataSchemes
        clone
    }
}

