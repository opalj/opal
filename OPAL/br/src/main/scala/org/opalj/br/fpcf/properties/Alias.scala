/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation

sealed trait AliasPropertyMetaInformation extends PropertyMetaInformation {
    type Self = Alias
}

/**
 * Describes the alias properties of the associated entities.
 *
 * Alias properties can establish a binding between a pair of entities, including definition sites, formal parameters, method return values, fields, etc.
 * For more information, see TODO.
 * The pair may consist of different types of entities, such as a field and a formal parameter.
 *
 * An alias property provides information about the relationship of the memory locations of the associated entities.
 *
 *  - [[NoAlias]] can be assigned to a pair of entities, iff no execution path exists where both entities refer to the same memory location.
 *
 *  - [[MustAlias]] can be assigned to a pair of entities, iff both entities refer to the same memory location in every execution time at all times.
 * This implies that at every use site of one entity, it can be replaced with the other entity (if it is defined at the current location)
 *
 *  - [[MayAlias]] can always be assigned to any given pair of entities without invalidating the results.
 *   It indicates that the two entities might refer to the same memory location but are not obligated to do so.
 *   This serves as a default/fallback value when no other property can be guaranteed.
 *
 * An analysis may only return [[MustAlias]] or [[NoAlias]] if it can guarantee that the property holds true under any circumstances.
 *
 * Alias information is only defined at a location where both entities of the associated pair are defined.
 * If one of the entities is not defined at the current location, the given alias property holds no information.
 */
sealed trait Alias extends AliasPropertyMetaInformation with Property {

  /**
   * A globally unique key used to access alias properties
   */
    final def key: PropertyKey[Alias] = Alias.key
}

object Alias extends AliasPropertyMetaInformation {

    /**
     * The name of the alias [[key]].
     */
    final val PropertyKeyName = "opalj.Alias"

    /**
     * The key used to access alias properties. It's name is "opalj.Alias" and the fallback value is [[MayAlias]].
     */
    final val key: PropertyKey[Alias] = {
          PropertyKey.create(
              PropertyKeyName,
              MayAlias
          )
      }
}

/**
 * Indicates that the two associated entities are guaranteed to '''always''' refer to the same memory location.
 *
 * @see [[Alias]] for more information about alias properties.
 */
case object MustAlias extends Alias

/**
 * Indicates that the two associated entities are guaranteed to '''never''' refer to the same memory location.
 *
 * @see [[Alias]] for more information about alias properties.
 */
case object NoAlias extends Alias

/**
 * Indicates that the two associated entities might refer to the same memory location but are not obligated to do so.
 *
 * This property does not guarantee that the actually relation of the associated entities can't be described using [[MustAlias]] or [[MayAlias]],
 * as there are scenarios where the analysis may not have sufficient information to prove a more specific relationship
 *
 * @see [[Alias]] for more information about alias properties.
 */
case object MayAlias extends Alias
