/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable.UIDSet

/**
 * Stores information about a type's supertypes.
 *
 * @author Michael Eichberg
 */
case class TypeDeclaration(
        objectType:             ObjectType,
        isInterfaceType:        Boolean,
        theSuperclassType:      Option[ObjectType],
        theSuperinterfaceTypes: UIDSet[ObjectType]
)
