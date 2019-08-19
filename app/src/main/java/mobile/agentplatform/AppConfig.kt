package mobile.agentplatform

import android.content.Context
import android.content.SharedPreferences

class AppConfig {
    private var context: Context
    private var sharedPreferences: SharedPreferences

    constructor(context: Context) {
        this.context = context
        sharedPreferences =
            context.getSharedPreferences(context.packageName + AppConstant.FILE_KEY_CONFIG, Context.MODE_PRIVATE)
    }

    fun get(key: String): String {
        return sharedPreferences.getString(key, "")
    }

    fun set(key: String, value: String): Boolean {
        return sharedPreferences.edit().putString(key, value).commit()
    }

    fun getPreferences(): SharedPreferences {
        return sharedPreferences
    }
}
