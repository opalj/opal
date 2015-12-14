/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package bugpicker
package core
package analysis

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType

/**
 * Identifies fields (static or instance) that are not used.
 *
 * @author Michael Eichberg
 */
object UnusedFields {

    def apply(
        theProject:    SomeProject,
        fieldAccessInformation : FieldAccessInformation,
        classFile : ClassFile
    ): Seq[StandardIssue] = {

        var issues = List.empty[StandardIssue]
        
        val candidateFields = classFile.fields.filterNot{field =>
            // these fields are inlined... hence, even if the field is not accessed it may
            // be used in the source code
            field.isFinal && (field.fieldType.isBaseType || (field.fieldType eq ObjectType.String))
                    }
        
        if(candidateFields.isEmpty)
            return Nil;
        
        
        val fieldsToAnalyze = theProject.analysisMode match {
            case AnalysisModes.DesktopApplication => candidateFields
            case AnalysisModes.JEE6WebApplication =>
                // TODO Refine
                candidateFields.filter(field => field.isPrivate)
                 case AnalysisModes.CPA => candidateFields.filter{field =>
                     !field.isPublic && (field.isProtected
                 }
                      case AnalysisModes.OPA => candidateFields.filter(field => field.isPrivate)
        }
            
        fieldAccessInformation.allReadAccesses.get(field) match {
            case None | Some(Seq()) =>
                if() {
                    // let's forget about the field... it is probably inlined..
                } else if (field.isPrivate || field.isProtected && theProject.analysisMode {
                    
                }
            case _ => // OK
        }
        }
        
//        StandardIssue(
//                    "UnusedLocalVariables",
//                    theProject,
//                    classFile,
//                    Some(method),
//                    if (vo >= 0) Some(vo) else None,
//                    None,
//                    None,
//                    issue,
//                    None,
//                    Set(IssueCategory.Smell, IssueCategory.Performance),
//                    Set(IssueKind.Useless),
//                    Nil,
//                    relevance
//                )
//            }
//        }

        issues

    }

}
