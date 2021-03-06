package mobile.agentplatform

import android.content.Intent

interface DrawerFragmentInterface {
    fun onNewIntent(intent: Intent?) = Unit
    fun onFragmentChange(extra: String?) = Unit
}
