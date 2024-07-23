package com.blinkllc.blinkappdemo.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.blinkapp.engine.company_user.view.InitializationResult
import com.blinkapp.engine.interfaces.*
import com.blinkapp.engine.managers.BlinkManager
import com.blinkapp.engine.utils.DriverActivitiesSettings
import com.blinkapp.engine.utils.DriverBehaviorType
import com.blinkapp.engine.utils.EndTripSettings
import com.blinkllc.blinkappdemo.MainActivity
import com.blinkllc.blinkappdemo.R
import com.blinkllc.blinkappdemo.notification.NotificationsHelper
import com.blinkllc.blinkappdemo.utils.Constants

class BlinkSDKService : Service(), TripEventListener,
    DriverBehaviorEventListener,
    AccidentEventListener,
    CallEventListener,
    HeadsetEventListener,
    ChargerEventListener,
    GPSChangesEventListener,
    BatteryLowEventListener,
    MockingEventListener,
    LocationBasedDriverBehaviorListener{
    private val TAG = "BlinkSDKService"
    private lateinit var blinkManager: BlinkManager


    companion object{
        var isBlinkSDKServiceRunning = false
    }
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BlinkSDKService = this@BlinkSDKService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        isBlinkSDKServiceRunning = true
        initializeBlinkManager()

    }
    private fun initializeBlinkManager() {
        blinkManager = BlinkManager.getInstance(this@BlinkSDKService)
        blinkManager.initialize(Constants.SECRET_KEY,object : InitializationResult {
            override fun onInitializationSuccess(message: String) {
                // Here You can start any modules you want
                Log.d(TAG, "onInitializationSuccess: "+ message)
//                blinkManager.startTrip(this@BlinkSDKService)
//                blinkManager.startTripWithLocation(this@BlinkSDKService)
                blinkManager.startSavingTripData(this@BlinkSDKService,"299",
                    withPoints = true,
                    withLiveTracking = true,
                    pointInterval = 5
                )
            }
            override fun onInitializationFailure(error: String) {
                //You can see the error of failure here
                Log.d(TAG, "onInitializationFailure: "+error)
            }

        })
    }

    private fun startAllModules(){
        blinkManager.startDriverBehavior(this@BlinkSDKService,3,
            withSavingIncident = true,
            calculateScore = true
        )
        blinkManager.startLocationBasedDriverBehaviorDetection(locationBasedDriverBehaviorListener = this, overSpeedLimit = 100.0, overSpeedThreshold = 5.0, withSaving = true)
        blinkManager.startAccident(this@BlinkSDKService, minimumSeverity = 3)
        blinkManager.startDriverActivitiesForCalls(this@BlinkSDKService, withSavingDriverActivity = true)
        blinkManager.startDriverActivitiesForHeadset(this@BlinkSDKService)
        blinkManager.startDriverActivitiesForCharging(this@BlinkSDKService,withSavingDriverActivity = true)
        blinkManager.startDriverActivitiesStatusOfGPS(this@BlinkSDKService,withSavingDriverActivity = true)
        blinkManager.startDriverActivitiesForBatteryLow(this@BlinkSDKService, minimumBatteryLevel = 3, periodWithSecondForBatteryLevel = 60,withSavingDriverActivity = true)
//        val  location = Location("")
//        location.latitude = 30.588
//        location.longitude = 31.954
//        blinkManager.addLocationForMocking(location)
//        blinkManager.startDriverActivitiesForMocking(this@BlinkSDKService,location)

    }
    private fun stopAllModules(){
        blinkManager.stopTripWithLocation()
        blinkManager.stopDriverBehavior()
        blinkManager.stopAccident()
        blinkManager.stopDriverActivities(DriverActivitiesSettings.CALLS_STATE)
        blinkManager.stopDriverActivities(DriverActivitiesSettings.HEADSET_STATE)
        blinkManager.stopDriverActivities(DriverActivitiesSettings.CHARGING_STATE)
        blinkManager.stopDriverActivities(DriverActivitiesSettings.GPS_STATE)
        blinkManager.stopDriverActivities(DriverActivitiesSettings.BATTERY_STATE)
//        blinkManager.stopDriverActivities(DriverActivitiesSettings.MOCKING_STATE)
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
        isBlinkSDKServiceRunning = false
        //blinkManager.stopTrip()
        //blinkManager.stopTripWithLocation()
        blinkManager.stopSavingTripData(EndTripSettings.DESTROY_SERVICE)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
         super.onStartCommand(intent, flags, startId)
        startAsForegroundService()
        return START_STICKY
    }

    /**
     * Promotes the service to a foreground service, showing a notification to the user.
     *
     * This needs to be called within 10 seconds of starting the service or the system will throw an exception.
     */
    private fun startAsForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create the notification channel
            NotificationsHelper.createNotificationChannel(this)

            // promote service to foreground service
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            startForeground(
//                1,
//                NotificationsHelper.buildNotification(this),
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
//                } else {
//                    0
//                }
//            )
//        }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    startForeground(
                        2000,
                        NotificationsHelper.buildNotification(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    )
                } catch (e: Exception) {
                    Log.d(TAG, "startForegroundIfRequiredExceptionMooo: " + e.message)
                }
            } else {
                startForeground(2000, NotificationsHelper.buildNotification(this))
            }
        }
    }

    override fun onTripStarted() {
        NotificationsHelper.showNotificationForEvents(
            "Trip",
            getString(R.string.trip_notification_start),
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
        startAllModules()
    }

    override fun onTripEnded() {
        NotificationsHelper.showNotificationForEvents(
            getString(R.string.app_name),
            getString(R.string.trip_notification_end),
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
        stopAllModules()

    }


    override fun onTripFailed(message: String?) {
        Toast.makeText(this@BlinkSDKService, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDriverBehaviorDetected(type:Int,level:Int) {
        Log.d(TAG, "onDriverBehaviorChangeMooo: "+type+"  level : "+level)
        NotificationsHelper.showNotificationForEvents(
            "Driver Behaviour",
            "Incident type ${getString(getIncidentName(type))} , level $level",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )    }
    private fun getIncidentName(type: Int): Int {
        return when (type) {
            DriverBehaviorType.BRAKE.id -> DriverBehaviorType.BRAKE.text
            DriverBehaviorType.CORNERING.id -> DriverBehaviorType.CORNERING.text
            DriverBehaviorType.SWERVE.id -> DriverBehaviorType.SWERVE.text

            else -> 0
        }
    }

    override fun onDriverBehaviorFailure(error: String) {
        Log.d(TAG, "onDriverBehaviourFailureMoo: "+error)
        Toast.makeText(this@BlinkSDKService, error, Toast.LENGTH_SHORT).show()
    }

    override fun onAccidentDetected(severity: Int) {
        Log.d(TAG, "onAccidentDetectedMooo: "+severity)
        NotificationsHelper.showNotificationForEvents(
            "Accident",
            "Accident with severity ${severity}",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    // if user confirm accident you may use this method to update and end trip in case of use trip for saving only
//        blinkManager.stopSavingTripData(EndTripSettings.CONFIRMED_ACCIDENT)
    }

    override fun onAccidentFailed(error: String) {
        Log.d(TAG, "onAccidentFailed: "+error)
    }


    override fun onCallStateDetected(state: Int) {
        Log.d(TAG, "onCallStateChanged: "+state)
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Calling)",
            getCallState(state),
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }
    private fun getCallState(callStatus:Int) =
        when(callStatus) {
            1 -> {
                "Make Call"
            }
            2->{
                "Incoming Call"
            }
            else -> {""}
        }
    override fun onCallDurationDetected(duration: Long) {
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Calling Duration)",
            "Duration $duration second",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )       }

    override fun onCallStateFailed(error: String) {
        Log.d(TAG, "onCallStateFailed: "+error)
    }

    override fun onHeadsetFailed(error: String) {
        Log.d(TAG, "onHeadsetFailed: "+error)
    }

    override fun onHeadsetStateDetected(pluggedIn: Boolean) {
        Log.d(TAG, "onHeadsetStateChanged: "+pluggedIn)
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Headset)",
            getHeadSetName(pluggedIn),
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }
    private fun getHeadSetName(pluggedIn: Boolean):String{
        return if (pluggedIn){
            "Plugged In"
        }
        else{
            "Unplugged"
        }
    }

    override fun onChargingFailed(error: String) {
        Log.d(TAG, "onChargingFailed: "+error)
    }

    override fun onChargingStateDetected(isCharging: Boolean) {
        Log.d(TAG, "onChargingStateChanged: "+isCharging)
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Charging)",
            getChargingName(isCharging),
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }
    private fun getChargingName(isCharging: Boolean):String{
        return if (isCharging){
            "Charging"
        }
        else{
            "Discharging"
        }
    }

    override fun onGPSFailed(error: String) {
        Log.d(TAG, "onGPSFailed: "+error)
    }

    override fun onGPSStateDetected(isGPSChanges: Boolean) {
        Log.d(TAG, "onGPSStateChanged: "+isGPSChanges)
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (GPS Changes)",
            "isGPSChanges $isGPSChanges",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }

    override fun onBatteryLowFailed(error: String) {
        Log.d(TAG, "onBatteryLowFailed: "+error)
    }

    override fun onBatteryLowDetected() {
        Log.d(TAG, "onBatteryLowStateChanged: ")
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Battery Low)",
            "Battery is low",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )     }

    override fun onMockingFailed(error: String) {
        Log.d(TAG, "onMockingFailed: "+error)
    }

    override fun onMockingStateDetected(isMocking: Boolean) {
        NotificationsHelper.showNotificationForEvents(
            "Driver Activities (Mocking)",
            "Is Mocking $isMocking",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }

    override fun onOverSpeedDetected(overSpeed: Float) {
        NotificationsHelper.showNotificationForEvents(
            "New Driver Behavior(Over Speed)",
            "OverSpeed $overSpeed",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )
    }

    override fun onAccelerationDetected() {
        NotificationsHelper.showNotificationForEvents(
            "New Driver Behavior(Over Speed)",
            "Acceleration ",
            MainActivity::class.java, false,0,this@BlinkSDKService
        )    }

    override fun onLocationBasedDriverBehaviorFailed(error: String) {
        Log.d(TAG, "onNewDriverBehaviorFailed: "+error)
    }




}