/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

sealed trait LibraryEntryPointsFakePropertyMetaInformation extends PropertyMetaInformation {
    final type Self = LibraryEntryPointsFakeProperty
}

/**
 * A fake property used internally to allow dependencies to the tac property in the loaded classes
 * analyses.
 *
 * @author Florian Kuebler
 */
sealed abstract class LibraryEntryPointsFakeProperty
        extends Property with LibraryEntryPointsFakePropertyMetaInformation {

    override def key: PropertyKey[LibraryEntryPointsFakeProperty] =
        LibraryEntryPointsFakeProperty.key
}

object LibraryEntryPointsFakePropertyFinal extends LibraryEntryPointsFakeProperty

object LibraryEntryPointsFakePropertyNonFinal extends LibraryEntryPointsFakeProperty

object LibraryEntryPointsFakeProperty extends LibraryEntryPointsFakePropertyMetaInformation {
    final val key: PropertyKey[LibraryEntryPointsFakeProperty] = {
        PropertyKey.forSimpleProperty(
            "LibraryEntryPointsFakeProperty", LibraryEntryPointsFakePropertyFinal
        )
    }
}
