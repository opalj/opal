/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.MethodDescriptor
import org.opalj.br.ReferenceType

case class CallSite(
        pc: Int, methodName: String, methodDescriptor: MethodDescriptor, receiver: ReferenceType
)