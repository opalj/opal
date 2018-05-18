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
package org.opalj
package hermes
package queries

import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_PROTECTED
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.FieldAccessInformationKey

/**
 * Counts how often fields are accessed.
 *
 * @author Michael Eichberg
 */
class FieldAccessStatistics(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    override def featureIDs: List[String] = {
        List(
            /*0*/ "unused private fields",
            /*1*/ "unused package visible fields",
            /*2*/ "unused protected fields",
            /*3*/ "unused public fields",
            /*4*/ "package visible fields\nonly used by defining type",
            /*5*/ "protected fields\nonly used by defining type",
            /*6*/ "public fields\nonly used by defininig type "
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(da.ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {
        val locations = Array.fill(featureIDs.size)(new LocationsContainer[S])

        val fieldAccessInformation = project.get(FieldAccessInformationKey)
        import fieldAccessInformation.isAccessed
        import fieldAccessInformation.allAccesses

        for {
            cf ← project.allProjectClassFiles
            classFileLocation = ClassFileLocation(project, cf)
            field ← cf.fields
            fieldType = field.fieldType
            if !fieldType.isBaseType ||
                (field.fieldType ne ObjectType.String) ||
                !(field.isStatic && field.isFinal)
        } {
            val category =
                if (!isAccessed(field)) {
                    field.visibilityModifier match {
                        case Some(ACC_PRIVATE)   ⇒ 0
                        case None                ⇒ 1
                        case Some(ACC_PROTECTED) ⇒ 2
                        case Some(ACC_PUBLIC)    ⇒ 3
                    }
                } else if (!field.isPrivate && allAccesses(field).forall(mi ⇒ mi._1.classFile eq cf)) {
                    field.visibilityModifier match {
                        case None                ⇒ 4
                        case Some(ACC_PROTECTED) ⇒ 5
                        case Some(ACC_PUBLIC)    ⇒ 6

                        case Some(ACC_PRIVATE) ⇒
                            throw new UnknownError(s"non private-field $field has private modifier")
                    }
                } else {
                    -1
                }
            if (category != -1) locations(category) += FieldLocation(classFileLocation, field)
        }
        locations
    }
}
