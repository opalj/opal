/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

/**
 * @param  featureID The id of the feature that is reued.
 * @param  featureQueryA The id of some feature query that derives features with
 *         the given name.
 * @param  featureQueryB The id of another feature query that derives features with
 *         the given name.
 */
case class DuplicateFeatureIDException(
        featureID:     String,
        featureQueryA: FeatureQuery,
        featureQueryB: FeatureQuery
) extends Exception(s"$featureID is derived by ${featureQueryA.id} and by ${featureQueryB.id}")
