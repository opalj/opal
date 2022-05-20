/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * Constant pool related type definitions.
 */
trait Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Constant_Pool_Entry <: ConstantPoolEntry
    implicit val constantPoolEntryType: ClassTag[Constant_Pool_Entry]

    type CONSTANT_Utf8_info <: Constant_Pool_Entry

    final type Constant_Pool = Array[Constant_Pool_Entry]

    final type Constant_Pool_Index = Int

    // The following definitions were introduced to enable the post transformation
    // of a class file after it was (successfully) loaded. In particular to resolve
    // references to the `BootstrapMethods` attribute.
    // The underlying idea is that the class file specific transformations are stored
    // in the class file's constant pool. The constant pool is basically loaded
    // first and then passed to all subsequently loaded class file elements
    // (Methods, Fields, ...)

    type ClassFile

    /**
     * A DeferredActionsStore stores all functions that need to perform post load actions.
     *
     * One example is the resolution of references to attributes.
     * (The constant pool is the only structure that is passed around and hence it is the
     * only place where to store information/functions related to a specific class file).
     */
    type DeferredActionsStore = mutable.Buffer[ClassFile => ClassFile] with Constant_Pool_Entry

    /**
     * This method is called/needs to be called after the class file was completely
     * loaded to perform class file specific transformations.
     */
    def applyDeferredActions(cp: Constant_Pool, classFile: ClassFile): ClassFile

}
