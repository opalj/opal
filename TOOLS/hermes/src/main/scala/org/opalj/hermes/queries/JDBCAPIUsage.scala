/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries

import org.opalj.br.ClassType
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

    override val apiFeatures: List[APIFeature] = {
        val DriverManager = ClassType("java/sql/DriverManager")
        val Connection = ClassType("java/sql/Connection")
        val Statement = ClassType("java/sql/Statement")
        val PreparedStatement = ClassType("java/sql/PreparedStatement")
        val CallableStatement = ClassType("java/sql/CallableStatement")

        List(
            StaticAPIMethod(DriverManager, "getConnection"),
            InstanceAPIMethod(Connection, "rollback"),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Connection, "createStatement"),
                    InstanceAPIMethod(Statement, "execute"),
                    InstanceAPIMethod(Statement, "executeQuery"),
                    InstanceAPIMethod(Statement, "executeUpdate")
                ),
                "creation and execution of\njava.sql.Statement"
            ),
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Connection, "prepareStatement"),
                    InstanceAPIMethod(PreparedStatement, "execute"),
                    InstanceAPIMethod(PreparedStatement, "executeQuery"),
                    InstanceAPIMethod(PreparedStatement, "executeUpdate")
                ),
                "creation and execution of\njava.sql.PreparedStatement"
            ),
            APIFeatureGroup(
                List(
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
