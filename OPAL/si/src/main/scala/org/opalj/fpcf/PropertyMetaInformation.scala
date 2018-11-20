/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

/**
 * The meta information shared by properties and their respective kinds.
 *
 * @author Michael Eichberg
 */
trait PropertyMetaInformation extends PropertyKind {

    type Self <: Property

    /**
     * The key uniquely identifies this property's category. All property objects
     * of the same kind have to use the same key.
     *
     * In general each `Property` kind is expected to have a companion object that
     * stores the unique `PropertyKey`.
     */
    def key: PropertyKey[Self]

    final def id: Int = key.id
}
