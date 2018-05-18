/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
