package org.opalj
package br

import scala.language.implicitConversions
import scala.collection.mutable.ArrayBuffer

package object cp {
    type Constant_Pool = Array[Constant_Pool_Entry]

    type BootstrapMethodsBuffer = ArrayBuffer[BootstrapMethod]

    type Constant_Pool_Index = Int

    implicit def cpIndexTocpEntry(
        index: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): Constant_Pool_Entry =
        cp(index)
}