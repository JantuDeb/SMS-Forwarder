package com.jantu.smsforward.receiver


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.jantu.smsforward.model.ForwardingMethod
import com.jantu.smsforward.model.UserSettings
import com.jantu.smsforward.util.PreferencesHelper

class SmsReceiver : BroadcastReceiver() {

    private companion object {
        const val TAG = "SmsReceiver"
        const val WHATSAPP_MESSAGE_PREFIX = "Forwarded SMS:\n"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: Recieved")
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return

            val settings = PreferencesHelper.loadSettings(context)
            if (settings.forwardingPhoneNumber.isBlank() || settings.triggerTexts.isEmpty() || settings.forwardingMethod == ForwardingMethod.NONE) {
                Log.d(TAG, "Forwarding not configured or disabled. Skipping.")
                return
            }

            val smsBody = StringBuilder()
            var sender: String? = null

            for (pdu in pdus) {
                val smsMessage =
                    SmsMessage.createFromPdu(pdu as ByteArray, bundle.getString("format"))
                smsBody.append(smsMessage.messageBody)
                if (sender == null) {
                    sender = smsMessage.displayOriginatingAddress
                }
            }

            val fullSmsText = smsBody.toString()
            Log.d(TAG, "SMS Received from: $sender Body: $fullSmsText")

            val matchedTrigger = settings.triggerTexts.firstOrNull { trigger ->
                fullSmsText.contains(trigger, ignoreCase = true)
            }

            if (matchedTrigger != null) {
                Log.i(TAG, "Trigger text '$matchedTrigger' found in SMS.")
                val messageToForward = "$WHATSAPP_MESSAGE_PREFIX" +
                        "From: $sender\n" +
                        "Message: $fullSmsText"

                handleForwarding(context, settings, messageToForward)
            } else {
                Log.d(TAG, "No trigger text found.")
            }
        }
    }

    private fun handleForwarding(context: Context, settings: UserSettings, message: String) {
        when (settings.forwardingMethod) {
            ForwardingMethod.WHATSAPP -> sendWhatsAppMessage(context, settings.forwardingPhoneNumber, message)
            ForwardingMethod.SMS -> sendSms(context, settings.forwardingPhoneNumber, message)
            ForwardingMethod.BOTH -> {
                sendWhatsAppMessage(context, settings.forwardingPhoneNumber, message)
                sendSms(context, settings.forwardingPhoneNumber, message)
            }
            ForwardingMethod.NONE -> Log.d(TAG, "Forwarding method is NONE. Not forwarding.")
        }
    }

    private fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String) {
        if (!isWhatsAppInstalled(context)) {
            Toast.makeText(context, "WhatsApp not installed.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "WhatsApp not installed.")
            return
        }

        // Ensure phone number has country code for WhatsApp. This is a basic check.
        val whatsappNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber" // Adjust if needed

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=$whatsappNumber&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.setPackage("com.whatsapp")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.i(TAG, "WhatsApp intent sent to $whatsappNumber")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending WhatsApp message: ${e.message}", e)
            Toast.makeText(context, "Error: Could not open WhatsApp.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
            // For long messages, split them
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            Log.i(TAG, "SMS sent to $phoneNumber")
            // Toast from background is generally discouraged, but for quick feedback:
            // Toast.makeText(context, "SMS forwarded to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS: ${e.message}", e)
            Toast.makeText(context, "Error: Could not send SMS.", Toast.LENGTH_LONG).show()
        }
    }

    private fun isWhatsAppInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}