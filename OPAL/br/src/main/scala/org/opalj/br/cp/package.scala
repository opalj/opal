/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import scala.language.implicitConversions

import scala.collection.mutable

/**
 * Implementation of classes to represent/recreate a class file's constant pool.
 *
 * @author Michael Eichberg
 */
package object cp {

    type Constant_Pool = Array[Constant_Pool_Entry]

    type BootstrapMethodsBuffer = mutable.ArrayBuffer[BootstrapMethod]

    type Constant_Pool_Index = Int

    implicit def cpIndexToCPEntry(
        index: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): Constant_Pool_Entry = {
        cp(index)
    }
}