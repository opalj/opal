/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the `type_path` field of type annotations. This
 * reader is intended to be used in conjunction with the
 * [[TypeAnnotationsReader]].
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationPathReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type TypeAnnotationPath <: AnyRef

    /**
     * The path's length was `0`.
     */
    def TypeAnnotationDirectlyOnType: TypeAnnotationPath

    type TypeAnnotationPathElement <: AnyRef
    implicit val typeAnnotationPathElementType: ClassTag[TypeAnnotationPathElement] // TODO: Replace in Scala 3 by `type TypeAnnotationPathElement : ClassTag`
    type TypeAnnotationPathElementsTable = ArraySeq[TypeAnnotationPathElement]

    def TypeAnnotationPath(path: TypeAnnotationPathElementsTable): TypeAnnotationPath

    /**
     * The `type_path_kind` was `0` (and the type_argument_index was also `0`).
     */
    def TypeAnnotationDeeperInArrayType: TypeAnnotationPathElement

    /**
     * The `type_path_kind` was `1` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationDeeperInNestedType: TypeAnnotationPathElement

    /**
     * The `type_path_kind` was `2` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationOnBoundOfWildcardType: TypeAnnotationPathElement

    def TypeAnnotationOnTypeArgument(type_argument_index: Int): TypeAnnotationPathElement

    //
    // IMPLEMENTATION
    //

    def TypeAnnotationPath(in: DataInputStream): TypeAnnotationPath = {
        val path_length = in.readUnsignedByte()
        if (path_length == 0) {
            TypeAnnotationDirectlyOnType
        } else {
            TypeAnnotationPath(
                fillArraySeq(path_length) {
                    val type_path_kind = in.readUnsignedByte()
                    (type_path_kind: @scala.annotation.switch) match {
                        // FROM THE JVM SPEC:
                        // If the value of the type_path_kind item is 0, 1, or 2,
                        // then the value of the type_argument_index item is 0.
                        case 0 =>
                            in.read() // <=> in.skip..
                            TypeAnnotationDeeperInArrayType
                        case 1 =>
                            in.read() // <=> in.skip..
                            TypeAnnotationDeeperInNestedType
                        case 2 =>
                            in.read() // <=> in.skip..
                            TypeAnnotationOnBoundOfWildcardType
                        case 3 =>
                            TypeAnnotationOnTypeArgument(in.readUnsignedByte())
                    }
                }
            )
        }
    }
}
