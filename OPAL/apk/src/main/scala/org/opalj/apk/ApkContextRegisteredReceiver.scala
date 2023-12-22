/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package apk

import org.opalj.UShort
import org.opalj.br.Method

import com.typesafe.config.Config

/**
 * Represents the occurrence of a context-registered Broadcast Receiver via call to registerReceiver().
 *
 * @param clazz the class name of the Broadcast Receiver, might be imprecise.
 * @param intentActions the list of intent actions that trigger this component / entry point, might be incomplete.
 * @param intentCategories the list of intent categories that trigger this component / entry point, might be incomplete.
 * @param method the method in which the registerReceiver() call was found.
 * @param callPc the PC value of the registerReceiver() call in the method.
 *
 * @author Nicolas Gross
 */
class ApkContextRegisteredReceiver(
        clazz:            String,
        intentActions:    Seq[String],
        intentCategories: Seq[String],
        val method:       Method,
        val callPc:       UShort
)(implicit config: Config)
    extends ApkComponent(ApkComponentType.BroadcastReceiver, clazz, intentActions, intentCategories)
