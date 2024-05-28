package com.blinkllc.blinkappdemo.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import com.blinkllc.blinkappdemo.MainActivity
import com.blinkllc.blinkappdemo.R
import com.blinkllc.blinkappdemo.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

internal object NotificationsHelper {

        private const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
                // create the notification channel
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.service_message),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun buildNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("")
                .setLargeIcon(largeIcon(context))
                .setSmallIcon(R.drawable.logo)
//                    .setDefaults(Notification.DEFAULT_VIBRATE)
                .setOngoing(true)
                .setNotificationSilent()
                .setContentText(context.getString(R.string.service_message))
                .setContentIntent(Intent(context, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
                })
                .build()
        }

    private fun largeIcon(context: Context): Bitmap? {
        return BitmapFactory.decodeResource(context.resources, R.drawable.logo)
    }

    fun showNotificationForEventsWithActions(
        channelId: String?, title: String?, body: String?, pendingIntent: PendingIntent?,
        withSound: Boolean, @RawRes soundFile: Int,
        actions: List<NotificationCompat.Action?>?, notificationId: Int,context: Context
    ) {
        val pattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
        var soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (withSound) {
            soundUri =
                Uri.parse("android.resource://" +context.packageName + "/" + soundFile)
        }
        val mNotificationBuilder = NotificationCompat.Builder(
            context,
            channelId!!
        )
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setSound(soundUri)
            .setContentText(body)
        val mNotificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "Push Notification"
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.setShowBadge(false)
            notificationChannel.vibrationPattern = pattern
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationChannel.setSound(soundUri, audioAttributes)
            mNotificationManager.createNotificationChannel(notificationChannel)
            mNotificationBuilder.setChannelId(channelId)
        } else {
            mNotificationBuilder.setAutoCancel(true)
                .setLights(Color.BLUE, 500, 500)
                .setVibrate(pattern)
        }
        if (pendingIntent != null) mNotificationBuilder.setContentIntent(pendingIntent)
        if (actions != null && !actions.isEmpty()) {
            for (action in actions) {
                mNotificationBuilder.addAction(action)
            }
        }
        mNotificationManager.notify(notificationId, mNotificationBuilder.build())
    }


    fun showNotificationForEvents(
        messageTitle: String?,
        messageBody: String?,
        target: Class<*>?,
        sound: Boolean,
        soundFile: Int,
        context: Context
    ) {
        val channelId = "Blink SDK notifications"
        val now = Date()
        val notificationId = SimpleDateFormat("ddHHmmss", Locale.US).format(now).toInt()
        var pendingIntent: PendingIntent? = null
        if (target != null) {
            val intent = Intent(context, target)
            val extra = Bundle(1)
                extra.putInt(Constants.NOTIFICATION_ID, notificationId)
                intent.putExtras(extra)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            var flags = PendingIntent.FLAG_ONE_SHOT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags =
                flags or PendingIntent.FLAG_IMMUTABLE
            pendingIntent =
                PendingIntent.getActivity(context, notificationId, intent, flags)
        }
        val pattern = longArrayOf(
            1000,
            1000,
            1000,
            1000,
            1000
        ) //new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400}
        var soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (sound) {
            soundUri =
                Uri.parse("android.resource://" + context.getPackageName() + "/" + soundFile)
        }
        val mNotificationBuilder =
            NotificationCompat.Builder(context, "NOTIFICATION_CHANNEL_ID")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(messageTitle)
                .setSound(soundUri)
                .setContentText(messageBody)
        val mNotificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName: CharSequence = "Push Notification"
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(channelId, channelName, importance)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.setShowBadge(true)
            notificationChannel.enableVibration(true)
            notificationChannel.vibrationPattern = pattern
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationChannel.setSound(soundUri, audioAttributes)
            mNotificationManager.createNotificationChannel(notificationChannel)
            mNotificationBuilder.setChannelId(channelId!!)
        } else {
            mNotificationBuilder.setAutoCancel(true)
                .setLights(Color.BLUE, 500, 500)
                .setVibrate(pattern)
        }
        if (pendingIntent != null) mNotificationBuilder.setContentIntent(pendingIntent)


//        mNotificationManager.notify(1 /* ID of notification */, mNotificationBuilder.build());
        mNotificationManager.notify(
            notificationId /* ID of notification */,
            mNotificationBuilder.build()
        )
    }

}