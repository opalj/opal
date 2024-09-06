/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br._
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod

/**
 * Groups features that use reflection to modify field values.
 *
 * @author Dominik Helm
 */
class ReflectiveFieldWrites(implicit hermes: HermesConfig) extends APIFeatureQuery {

    val FieldT = ObjectType("java/lang/reflect/Field")
    val Lookup = ObjectType.MethodHandles$Lookup

    override val apiFeatures: List[APIFeature] = {
        List(
            InstanceAPIMethod(FieldT, "set", featureID = "FieldSet"),
            InstanceAPIMethod(FieldT, "setBoolean", featureID = "FieldSetBoolean"),
            InstanceAPIMethod(FieldT, "setByte", featureID = "FieldSetByte"),
            InstanceAPIMethod(FieldT, "setChar", featureID = "FieldSetChar"),
            InstanceAPIMethod(FieldT, "setDouble", featureID = "FieldSetDouble"),
            InstanceAPIMethod(FieldT, "setFloat", featureID = "FieldSetFloat"),
            InstanceAPIMethod(FieldT, "setInt", featureID = "FieldSetInt"),
            InstanceAPIMethod(FieldT, "setLong", featureID = "FieldSetLong"),
            InstanceAPIMethod(FieldT, "setShort", featureID = "FieldSetShort"),
            InstanceAPIMethod(Lookup, "findStaticSetter", featureID = "MethodHandleStaticSetter"),
            InstanceAPIMethod(Lookup, "findSetter", featureID = "MethodHandleSetter")
        )
    }

}
