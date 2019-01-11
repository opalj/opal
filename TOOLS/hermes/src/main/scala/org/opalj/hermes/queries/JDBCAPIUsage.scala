/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes
package queries

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 *  Counts the amount of calls to certain JDBC api methods
 *
 * @author Michael Reif
 */
class JDBCAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: Chain[APIFeature] = {
        val DriverManager = ObjectType("java/sql/DriverManager")
        val Connection = ObjectType("java/sql/Connection")
        val Statement = ObjectType("java/sql/Statement")
        val PreparedStatement = ObjectType("java/sql/PreparedStatement")
        val CallableStatement = ObjectType("java/sql/CallableStatement")

        Chain(

            StaticAPIMethod(DriverManager, "getConnection"),
            InstanceAPIMethod(Connection, "rollback"),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Connection, "createStatement"),
                    InstanceAPIMethod(Statement, "execute"),
                    InstanceAPIMethod(Statement, "executeQuery"),
                    InstanceAPIMethod(Statement, "executeUpdate")
                ),
                "creation and execution of\njava.sql.Statement"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Connection, "prepareStatement"),
                    InstanceAPIMethod(PreparedStatement, "execute"),
                    InstanceAPIMethod(PreparedStatement, "executeQuery"),
                    InstanceAPIMethod(PreparedStatement, "executeUpdate")
                ),
                "creation and execution of\njava.sql.PreparedStatement"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Connection, "prepareCall"),
                    InstanceAPIMethod(CallableStatement, "execute"),
                    InstanceAPIMethod(CallableStatement, "executeQuery"),
                    InstanceAPIMethod(CallableStatement, "executeUpdate")
                ),
                "creation and execution of\njava.sql.CallableStatement"
            )
        )
    }
}
