/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import org.opalj.UShort
import org.opalj.br.Method

/**
 * Represents the occurrence of a context-registered Broadcast Receiver via call to registerReceiver().
 *
 * @param clazz the class name of the Broadcast Receiver, might be imprecise.
 * @param intents the list of intents that triggers this component / entry point, might be incomplete.
 * @param method the method in which the registerReceiver() call was found.
 * @param callPc the PC value of the registerReceiver() call in the method.
 *
 * @author Nicolas Gross
 */
class ApkContextRegisteredReceiver(clazz: String,
                                   intents: Seq[String],
                                   val method: Method,
                                   val callPc: UShort)
    extends ApkComponent(ApkComponentType.BroadcastReceiver, clazz, intents)
