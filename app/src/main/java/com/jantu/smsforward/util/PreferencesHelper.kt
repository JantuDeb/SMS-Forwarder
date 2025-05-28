package com.jantu.smsforward.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jantu.smsforward.model.ForwardingMethod
import com.jantu.smsforward.model.UserSettings

object PreferencesHelper {
    private const val PREFS_NAME = "sms_forwarder_prefs"
    private const val KEY_TRIGGER_TEXTS = "trigger_texts"
    private const val KEY_FORWARDING_NUMBER = "forwarding_number"
    private const val KEY_FORWARDING_METHOD = "forwarding_method"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSettings(context: Context, settings: UserSettings) {
        getPreferences(context).edit {
            putString(KEY_TRIGGER_TEXTS, settings.triggerTexts.joinToString(","))
            putString(KEY_FORWARDING_NUMBER, settings.forwardingPhoneNumber)
            putString(KEY_FORWARDING_METHOD, settings.forwardingMethod.name)
        }
    }

    fun loadSettings(context: Context): UserSettings {
        val prefs = getPreferences(context)
        val triggerTextsString = prefs.getString(KEY_TRIGGER_TEXTS, "") ?: ""
        val triggers = if (triggerTextsString.isNotBlank()) {
            triggerTextsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }
        val phoneNumber = prefs.getString(KEY_FORWARDING_NUMBER, "") ?: ""
        val methodName = prefs.getString(KEY_FORWARDING_METHOD, ForwardingMethod.NONE.name) ?: ForwardingMethod.NONE.name
        val method = try {
            ForwardingMethod.valueOf(methodName)
        } catch (e: IllegalArgumentException) {
            ForwardingMethod.NONE
        }

        return UserSettings(triggers, phoneNumber, method)
    }
}