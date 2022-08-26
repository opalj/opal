/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.apk

import org.opalj.apk.ApkComponentType.ApkComponentType

/**
 * Component of an APK. Each component is a potential entry point. A component is either an
 * Activity, Service, Broadcast Receiver or Content Provider. Parsed from AndroidManifest.xml.
 *
 * The entry point functions were collected up to API level 33 (Android 13). They also include
 * all deprecated functions.
 *
 * @param componentType the type of the component.
 * @param clazz the class name of the component.
 * @param intentActions list of intent actions that trigger this component / entry point.
 * @param intentCategories list of intent categories that trigger this component / entry point.
 *
 * @author Nicolas Gross
 */
class ApkComponent(val componentType: ApkComponentType,
                   val clazz: String,
                   val intentActions: Seq[String],
                   val intentCategories: Seq[String]) {

    private val ActivityEntryPoints = Seq("onActionModeFinished", "onActionModeStarted", "onActivityReenter",
        "onAttachFragment", "onAttachedToWindow", "onBackPressed", "onConfigurationChanged", "onContentChanged",
        "onContextItemSelected", "onContextMenuClosed", "onCreate", "onCreateContextMenu", "onCreateDescription",
        "onCreateNavigateUpTaskStack", "onCreateOptionsMenu", "onCreatePanelMenu", "onCreatePanelView",
        "onCreateThumbnail", "onCreateView", "onDetachedFromWindow", "onEnterAnimationComplete", "onGenericMotionEvent",
        "onGetDirectActions", "onKeyDown", "onKeyLongPress", "onKeyMultiple", "onKeyShortcut", "onKeyUp",
        "onLocalVoiceInteractionStarted", "onLocalVoiceInteractionStopped", "onLowMemory", "onMenuItemSelected",
        "onMenuOpened", "onMultiWindowModeChanged", "onNavigateUp", "onNavigateUpFromChild", "onOptionsItemSelected",
        "onOptionsMenuClosed", "onPanelClosed", "onPerformDirectAction", "onPictureInPictureModeChanged",
        "onPictureInPictureRequested", "onPictureInPictureUiStateChanged", "onPostCreate", "onPrepareNavigateUpTaskStack",
        "onPrepareOptionsMenu", "onPreparePanel", "onProvideAssistContent", "onProvideAssistData",
        "onProvideKeyboardShortcuts", "onProvideReferrer", "onRequestPermissionsResult", "onRestoreInstanceState",
        "onRetainNonConfigurationInstance", "onSaveInstanceState", "onSearchRequested", "onStateNotSaved",
        "onTopResumedActivityChanged", "onTouchEvent", "onTrackballEvent", "onTrimMemory", "onUserInteraction",
        "onVisibleBehindCanceled", "onWindowAttributesChanged", "onWindowFocusChanged", "onWindowStartingActionMode",
        "onActivityResult", "onApplyThemeResource", "onChildTitleChanged", "onCreateDialog", "onDestroy", "onNewIntent",
        "onPause", "onPostCreate", "onPostResume", "onPrepareDialog", "onRestart", "onResume", "onStart", "onStop",
        "onTitleChanged", "onUserLeaveHint")
    private val ServiceEntryPoints = Seq("onBind", "onConfigurationChanged", "onCreate", "onDestroy", "onLowMemory",
        "onRebind", "onStart", "onStartCommand", "onTaskRemoved", "onTrimMemory", "onUnbind")
    private val ReceiverEntryPoints = Seq("onReceive")
    private val ProviderEntryPoints = Seq("onCallingPackageChanged", "onConfigurationChanged", "onCreate", "onLowMemory",
        "onTrimMemory", "applyBatch", "bulkInsert", "call", "canonicalize", "delete", "getStreamTypes", "getType",
        "insert", "openAssetFile", "openFile", "openTypedAssetFile", "query", "refresh", "shutdown", "update")

    /**
     * Returns the list of functions that might be called as entry points for this component.
     */
    def entryFunctions(): Seq[String] = {
        componentType match {
            case ApkComponentType.Activity => ActivityEntryPoints
            case ApkComponentType.Service => ServiceEntryPoints
            case ApkComponentType.BroadcastReceiver => ReceiverEntryPoints
            case ApkComponentType.ContentProvider => ProviderEntryPoints
        }
    }

    override def toString: String = {
        val ls = System.lineSeparator()
        s"$clazz: $componentType$ls\tactions: $intentActions$ls\tcategories: $intentCategories$ls"
    }
}

