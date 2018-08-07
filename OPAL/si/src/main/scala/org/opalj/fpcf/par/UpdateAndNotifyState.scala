/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package par

sealed trait UpdateAndNotifyState {
    def isNotificationRequired: Boolean
    def areDependersNotified: Boolean
}
case object NoRelevantUpdate extends UpdateAndNotifyState {
    override def isNotificationRequired: Boolean = false
    override def areDependersNotified: Boolean = false
}
case object RelevantUpdateButNoNotification extends UpdateAndNotifyState {
    override def isNotificationRequired: Boolean = true
    override def areDependersNotified: Boolean = false
}
case object RelevantUpdateAndNotification extends UpdateAndNotifyState {
    override def isNotificationRequired: Boolean = false
    override def areDependersNotified: Boolean = true
}
