package com.blinkllc.blinkappdemo

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.blinkapp.engine.company_user.view.InitializationResult
import com.blinkapp.engine.interfaces.*
import com.blinkapp.engine.managers.BlinkManager
import com.blinkapp.engine.utils.Constants
import com.blinkapp.engine.utils.DriverActivitiesSettings
import com.blinkllc.blinkappdemo.databinding.ActivityMainBinding
import com.blinkllc.blinkappdemo.service.BlinkSDKService
import com.blinkllc.blinkappdemo.utils.Constants.SECRET_KEY
import com.blinkllc.blinkappdemo.utils.PermissionManager
import com.blinkllc.blinkappdemo.utils.PermissionManager.REQUEST_BACKGROUND_LOCATION_PERMISSIONS
import com.blinkllc.blinkappdemo.utils.PermissionManager.REQUEST_CODE_PERMISSIONS
import com.blinkllc.blinkappdemo.utils.PermissionManager.REQUEST_LOCATION_PERMISSIONS
import com.blinkllc.blinkappdemo.utils.PermissionManager.isAccessFineAndCoarseLocationPermissionsGranted
import com.blinkllc.blinkappdemo.utils.PermissionManager.manageBatteryOptimization
import com.blinkllc.blinkappdemo.utils.PermissionManager.requestBackgroundLocationPermissions
import com.blinkllc.blinkappdemo.utils.PermissionManager.requestLocationPermissions

class MainActivity : AppCompatActivity(),
    MockingEventListener{
    private val TAG = "MainActivity"
    private lateinit var activityMainBinding: ActivityMainBinding
    private lateinit var blinkManager: BlinkManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        requestPermissions()
        initializeBlinkManager()
        handleForegroundServiceSwitch()
        handleTripModule()
        handleDriverBehaviourModule()
        handleAccidentModule()
        // driver activities
        handleMakeCall()
        handleHeadset()
        handleCharging()
        handleGPS()
        handleBattery()
        handleMocking()


    }
    private fun startBlinkSDKService() {
        Log.d(TAG, "startBlinkServiceigdggkdnkgd: "+isAccessFineAndCoarseLocationPermissionsGranted(this@MainActivity))
        if (isAccessFineAndCoarseLocationPermissionsGranted(this@MainActivity)){
            val intent = Intent(this, BlinkSDKService::class.java)
            Log.d(TAG, "serviceMoooHome: ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }

    }
    private fun stopBlinkSDKService(){
        val intent = Intent(this@MainActivity, BlinkSDKService::class.java)
        stopService(intent)
    }

    override fun onResume() {
        super.onResume()
        activityMainBinding.apply {
            serviceSwitch.isChecked = BlinkSDKService.isBlinkSDKServiceRunning
        }

    }


    private fun initializeBlinkManager() {
        blinkManager = BlinkManager.getInstance(this@MainActivity)
        blinkManager.initialize(SECRET_KEY,object :InitializationResult{
            override fun onInitializationSuccess(message: String) {
                // Here You can start any modules you want
                Log.d(TAG, "onInitializationSuccess: "+message)
            }
            override fun onInitializationFailure(error: String) {
                //You can see the error of failure here
                Log.d(TAG, "onInitializationFailure: "+error)
            }

        })
    }



    private fun handleForegroundServiceSwitch(){
        activityMainBinding.apply {
            serviceSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    startBlinkSDKService()
                    disableAllSwitches()
                } else {
                    stopBlinkSDKService()
                    enableAllSwitches()
                }
            }
        }
    }

    private fun disableAllSwitches(){
        activityMainBinding.apply {
            tripSwitch.isEnabled = false
            incidentSwitch.isEnabled = false
            accidentSwitch.isEnabled = false
            callSwitch.isEnabled = false
            headsetSwitch.isEnabled = false
            chargingSwitch.isEnabled = false
            gpsSwitch.isEnabled = false
            batterySwitch.isEnabled = false
            mockingSwitch.isEnabled = false
        }
    }

    private fun enableAllSwitches(){
        activityMainBinding.apply {
            tripSwitch.isEnabled = true
            incidentSwitch.isEnabled = true
            accidentSwitch.isEnabled = true
            callSwitch.isEnabled = true
            headsetSwitch.isEnabled = true
            chargingSwitch.isEnabled = true
            gpsSwitch.isEnabled = true
            batterySwitch.isEnabled = true
            mockingSwitch.isEnabled = true
        }
    }

    private fun handleTripModule(){
        activityMainBinding.apply {
            tripSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
//                blinkManager.startTripWithLocation(this@MainActivity)
                    blinkManager.startSavingTripData(object : TripEventListener {
                        /**
                         * a callback method called when trip started
                         */
                        override fun onTripStarted() {
                            updateStartTripStatus()
                            blinkManager.startDriverBehavior(object : DriverBehaviorEventListener {
                                /**
                                 * a callback method called when driver behavior updated.
                                 *
                                 * @param type the type of driver behavior
                                 * @param level the level of driver behavior
                                 */
                                override fun onDriverBehaviorDetected(type:Int , level:Int) {
                                    updateDriverBehaviour(type,level)
                                }
                                /**
                                 * Called when there is a failure in the drive behavior.
                                 *
                                 * @param error the description or details of the failure
                                 */
                                override fun onDriverBehaviorFailure(error: String) {
                                    runOnUiThread {
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                        activityMainBinding.incidentSwitch.isChecked = false
                                    }
                                }

                            },minimumIncidentLevel = 5,withSavingIncident = true, calculateScore = true)

                            blinkManager.startDriverActivitiesStatusOfGPS(object :GPSChangesEventListener{
                                /**
                                 * a callback method called when GPS state updated
                                 *
                                 * @param isGPSChanges the state of GPS
                                 */
                                override fun onGPSStateDetected(isGPSChanges: Boolean) {
                                    updateGPSStatus(isGPSChanges)
                                }
                                /**
                                 * Called when GPS State fails.
                                 *
                                 * @param error the description or details of the failure
                                 */
                                override fun onGPSFailed(error: String) {
                                    runOnUiThread {
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                        activityMainBinding.gpsSwitch.isChecked = false
                                    }
                                }

                            }, withSavingDriverActivity = true)


                            blinkManager.startDriverActivitiesForCalls(object:CallEventListener{
                                /**
                                 * a callback method called when phone call duration updated
                                 *
                                 * @param duration the duration of phone call
                                 */
                                override fun onCallDurationDetected(duration: Long) {
                                    updateCallStatusWithDuration(duration)
                                }
                                /**
                                 * a callback method called when phone call state updated
                                 *
                                 * @param state the state of phone call
                                 */
                                override fun onCallStateDetected(state: Int) {
                                    updateCallStatus(state)
                                }
                                /**
                                 * Called when Call State fails.
                                 *
                                 * @param error the description or details of the failure
                                 */
                                override fun onCallStateFailed(error: String) {
                                    runOnUiThread {
                                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                        activityMainBinding.callSwitch.isChecked = false
                                    }
                                }

                            }, withSavingDriverActivity = true)

                        }



                        /**
                         * a callback method called when trip ended
                         */
                        override fun onTripEnded() {
                            updateEndTripStatus()
                            blinkManager.stopDriverBehavior()
                            blinkManager.stopDriverActivitiesStatusOfGPS()
                        }

                        /**
                         * Called when a trip registration fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onTripFailed(error: String) {
                            Log.d(TAG, "onTripFailedaddas: "+error)
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.tripSwitch.isChecked = false
                            }
                        }

                    }, userId = "299", withPoints = true, withLiveTracking = true, pointInterval = 5)

//                blinkManager.startTrip(object : TripEventListener {
//                    /**
//                     * a callback method called when trip started
//                     */
//                    override fun onTripStarted() {
//
//                    }
//
//                    /**
//                     * a callback method called when trip ended
//                     */
//                    override fun onTripEnded() {
//
//                    }
//
//                    /**
//                     * Called when a trip registration fails.
//                     *
//                     * @param error the description or details of the failure
//                     */
//                    override fun onTripFailed(p0: String?) {
//
//                    }
//
//                })


                } else {
                    blinkManager.stopSavingTripData()
//                blinkManager.stopTripWithLocation()
//                blinkManager.stopTrip()

                }
            }
        }
    }
    private fun handleDriverBehaviourModule() {
        activityMainBinding.apply {
            incidentSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverBehavior(object : DriverBehaviorEventListener {
                        /**
                         * a callback method called when driver behavior updated.
                         *
                         * @param type the type of driver behavior
                         * @param level the level of driver behavior
                         */
                        override fun onDriverBehaviorDetected(type:Int , level:Int) {
                            updateDriverBehaviour(type,level)
                        }
                        /**
                         * Called when there is a failure in the drive behavior.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onDriverBehaviorFailure(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.incidentSwitch.isChecked = false
                            }
                        }

                    })
//                    blinkManager.startDriverBehavior(object : DriverBehaviorEventListener {
//                        /**
//                         * a callback method called when driver behavior updated.
//                         *
//                         * @param event the type of driver behavior
//                         * @param level the level of driver behavior
//                         */
//                        override fun onDriverBehaviorDetected(type:Int , level:Int) {
//                            updateDriverBehaviour(type,level)
//                        }
//                        /**
//                         * Called when there is a failure in the drive behavior.
//                         *
//                         * @param value the description or details of the failure
//                         */
//                        override fun onDriverBehaviourFailure(error: String) {
//                            runOnUiThread {
//                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
//                                activityMainBinding.incidentSwitch.isChecked = false
//                            }
//                        }
//
//                    },5)
                } else {
                    blinkManager.stopDriverBehavior()
                }
            }
        }
    }

    private fun handleAccidentModule(){
        activityMainBinding.apply {
            accidentSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startAccident(object :AccidentEventListener{
                        /**
                         * a callback method called when detect accident
                         *
                         * @param severity the properties of the accident severity level
                         */
                        override fun onAccidentDetected(severity: Int) {
                            updateDriverAccident(severity)
                            // if user confirm accident you may use this method to update and end trip in case of use start trip for saving only
                            // blinkManager.stopSavingTripData(EndTripSettings.CONFIRMED_ACCIDENT)
                        }
                        /**
                         * Called when accident fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onAccidentFailed(error: String?) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.accidentSwitch.isChecked = false
                            }
                        }

                    })
//                    blinkManager.startAccident(object :AccidentEventListener{
//                        /**
//                         * a callback method called when detect accident
//                         *
//                         * @param data the properties of the accident and severity level
//                         */
//                        override fun onAccidentDetected(accidentData: AccidentData) {
//                            updateDriverAccident(accidentData.severityLevel)
//                        }
//                        /**
//                         * Called when accident fails.
//                         *
//                         * @param error the description or details of the failure
//                         */
//                        override fun onAccidentFailed(error: String?) {
//                            runOnUiThread {
//                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
//                                activityMainBinding.accidentSwitch.isChecked = false
//                            }
//                        }
//
//                    },4)
                } else {
                    blinkManager.stopAccident()
                }
            }
        }
    }
    private fun handleMakeCall() {
        activityMainBinding.apply {
            callSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverActivitiesForCalls(object:CallEventListener{
                        /**
                         * a callback method called when phone call duration updated
                         *
                         * @param duration the duration of phone call
                         */
                        override fun onCallDurationDetected(duration: Long) {
                            updateCallStatusWithDuration(duration)
                        }
                        /**
                         * a callback method called when phone call state updated
                         *
                         * @param state the state of phone call
                         */
                        override fun onCallStateDetected(state: Int) {
                            updateCallStatus(state)
                        }
                        /**
                         * Called when Call State fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onCallStateFailed(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.callSwitch.isChecked = false
                            }
                        }

                    })
//
//                    blinkManager.startDriverActivities(DriverActivitiesSettings.CALLING_STATE,object:CallEventListener{
//                        /**
//                         * a callback method called when phone call duration updated
//                         *
//                         * @param duration the duration of phone call
//                         */
//                        override fun onCallDurationDetected(duration: Long) {
//                            TODO("Not yet implemented")
//                        }
//                        /**
//                         * a callback method called when phone call state updated
//                         *
//                         * @param state the state of phone call
//                         */
//                        override fun onCallStateDetected(state: Int) {
//                            TODO("Not yet implemented")
//                        }
//                        /**
//                         * Called when Call State fails.
//                         *
//                         * @param error the description or details of the failure
//                         */
//                        override fun onCallStateFailed(error: String) {
//                            TODO("Not yet implemented")
//                        }
//
//                    })


                } else {
                    blinkManager.stopDriverActivitiesForCalls()
//                    blinkManager.stopDriverActivities(DriverActivitiesSettings.CALLING_STATE)
                }
            }
        }
    }
    private fun handleHeadset() {
        activityMainBinding.apply {
            headsetSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverActivitiesForHeadset(object :HeadsetEventListener{
                        /**
                         * a callback method called when headset state updated
                         *
                         * @param pluggedIn the state of headset
                         */
                        override fun onHeadsetStateDetected(pluggedIn: Boolean) {
                            updateHeadsetStatus(pluggedIn)
                        }
                        /**
                         * Called when Headset State fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onHeadsetFailed(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.headsetSwitch.isChecked = false
                            }
                        }

                    })
//                    blinkManager.startDriverActivities(DriverActivitiesSettings.HEADSET_STATE, headsetEventListener = object :HeadsetEventListener{
//
//                        override fun onHeadsetStateDetected(pluggedIn: Boolean) {
//                        }
//                        override fun onHeadsetFailed(error: String) {
//                        }
//
//
//                    })
                }
                else {
                    blinkManager.stopDriverActivitiesForHeadset()
//                    blinkManager.stopDriverActivities(DriverActivitiesSettings.HEADSET_STATE)
                }
            }
        }
    }
    private fun handleCharging() {
        activityMainBinding.apply {
            chargingSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverActivitiesForCharging(object :ChargerEventListener{
                        /**
                         * a callback method called when charging state updated
                         *
                         * @param isCharging the state of charging
                         */
                        override fun onChargingStateDetected(isCharging: Boolean) {
                            updateChargingStatus(isCharging)
                        }
                        /**
                         * Called when Charging State fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onChargingFailed(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.chargingSwitch.isChecked = false
                            }
                        }
                    })
//                    blinkManager.startDriverActivities(DriverActivitiesSettings.CHARGING_STATE, chargerEventListener = object:ChargerEventListener{
//                        /**
//                         * a callback method called when charging state updated
//                         *
//                         * @param isCharging the state of charging
//                         */
//                        override fun onChargingStateDetected(isCharging: Boolean) {
//                        }
//                        /**
//                         * Called when Charging State fails.
//                         *
//                         * @param error the description or details of the failure
//                         */
//                        override fun onChargingFailed(error: String) {
//                        }
//                    })
                } else {
                    blinkManager.stopDriverActivitiesForCharging()
                    blinkManager.stopDriverActivities(DriverActivitiesSettings.CHARGING_STATE)
                }
            }
        }
    }

    private fun handleGPS() {
        activityMainBinding.apply {
            gpsSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverActivitiesStatusOfGPS(object :GPSChangesEventListener{
                        /**
                         * a callback method called when GPS state updated
                         *
                         * @param isGPSChanges the state of GPS
                         */
                        override fun onGPSStateDetected(isGPSChanges: Boolean) {
                            updateGPSStatus(isGPSChanges)
                        }
                        /**
                         * Called when GPS State fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onGPSFailed(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.gpsSwitch.isChecked = false
                            }
                        }

                    })

//                    blinkManager.startDriverActivities(DriverActivitiesSettings.GPS_STATE, gpsChangesEventListener = object :GPSChangesEventListener{
//                        override fun onGPSStateDetected(isGPSChanges: Boolean) {
//
//                        }
//                        override fun onGPSFailed(error: String) {
//
//                        }
//
//
//                    })

                } else {
                    blinkManager.stopDriverActivitiesStatusOfGPS()
//                    blinkManager.stopDriverActivities(DriverActivitiesSettings.GPS_STATE)
                }
            }
        }
    }

    private fun handleBattery() {
        activityMainBinding.apply {
            batterySwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    blinkManager.startDriverActivitiesForBatteryLow(object :BatteryLowEventListener{
                        /**
                         * a callback method called when Battery Low detected
                         *
                         */
                        override fun onBatteryLowDetected() {
                            updateBatteryLowStatus()
                        }
                        /**
                         * Called when battery low fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onBatteryLowFailed(error: String) {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                                activityMainBinding.batterySwitch.isChecked = false
                            }
                        }
                    },/*minimumBatteryLevel*/15,/*periodWithSecond*/10)

//                    blinkManager.addBatteryLowConfiguration(10,10)
//                   blinkManager.startDriverActivities(DriverActivitiesSettings.BATTERY_STATE, batteryLowEventListener = object :BatteryLowEventListener{
//                       /**
//                        * a callback method called when Battery Low detected
//                        *
//                        */
//                       override fun onBatteryLowDetected() {
//                       }
//                       /**
//                        * Called when battery low fails.
//                        *
//                        * @param error the description or details of the failure
//                        */
//                       override fun onBatteryLowFailed(error: String) {
//
//                       }
//                   })

                } else {
                    blinkManager.stopDriverActivitiesForBatteryLow()
//                    blinkManager.stopDriverActivities(DriverActivitiesSettings.BATTERY_STATE)
                }
            }
        }
    }
    private fun handleMocking() {
        activityMainBinding.apply {
            mockingSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    val  location = Location("")
                    location.latitude = 30.588
                    location.longitude = 31.954
                    blinkManager.startDriverActivitiesForMocking(object :MockingEventListener{
                        /**
                         * a callback method called when mocking state updated
                         *
                         * @param isMocking the state of mocking
                         */
                        override fun onMockingStateDetected(isMocking: Boolean) {
                            TODO("Not yet implemented")
                        }
                        /**
                         * Called when Mocking State fails.
                         *
                         * @param error the description or details of the failure
                         */
                        override fun onMockingFailed(error: String) {
                            TODO("Not yet implemented")
                        }
                    },location)
//                    blinkManager.addLocationForMocking(location)
//                    blinkManager.startDriverActivities(DriverActivitiesSettings.MOCKING_STATE, mockingEventListener = object :MockingEventListener{
//                        /**
//                         * a callback method called when mocking state updated
//                         *
//                         * @param isMocking the state of mocking
//                         */
//                        override fun onMockingStateDetected(isMocking: Boolean) {
//
//                        }
//                        /**
//                         * Called when Mocking State fails.
//                         *
//                         * @param error the description or details of the failure
//                         */
//                        override fun onMockingFailed(error: String) {
//                        }
//                    })

                } else {
                    blinkManager.stopDriverActivities(DriverActivitiesSettings.MOCKING_STATE)
                }
            }
        }
    }

    private fun updateStartTripStatus(){
        runOnUiThread {
            activityMainBinding.tripStatus.text = "On Trip"
        }

    }
    private fun updateEndTripStatus(){
        runOnUiThread {
            activityMainBinding.tripStatus.text = "Parked"
        }
    }

    private fun updateDriverBehaviour(type: Int,level: Int){
        runOnUiThread {
            when (type)
            {
                1 ->{
                    activityMainBinding.incidentStatus.text = "Brake - Level: $level"
                }
                4->{
                    activityMainBinding.incidentStatus.text = "Swerve - Level: $level"
                }
                15->{
                    activityMainBinding.incidentStatus.text = "Cornering - Level: $level"
                }
            }


        }
    }

    private fun updateDriverAccident(severity: Int){
        runOnUiThread {
            activityMainBinding.accidentStatus.text = "Severity : $severity"

        }
    }

    private fun updateCallStatus(callStatus:Int){
        runOnUiThread {
            activityMainBinding.callStatus.text = getCallState(callStatus)
        }

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
    private fun updateCallStatusWithDuration(duration:Long){
        runOnUiThread {
            activityMainBinding.callStatus.text = activityMainBinding.callStatus.text.toString() + " with duration "+duration
        }

    }
    private fun updateHeadsetStatus(pluggedIn:Boolean){
        runOnUiThread {
            if (pluggedIn){
                activityMainBinding.headsetStatus.text = "Plugged In"
            }
            else{
                activityMainBinding.headsetStatus.text = "Unplugged"
            }
        }

    }
    private fun updateChargingStatus(isCharging:Boolean){
        runOnUiThread {
            if (isCharging){
                activityMainBinding.chargingStatus.text = "Charging"
            }
            else{
                activityMainBinding.chargingStatus.text = "Discharging"
            }
        }

    }

    private fun updateGPSStatus(isGPSChanges:Boolean){
        runOnUiThread {
            if (isGPSChanges){
                activityMainBinding.gpsStatus.text = "Enable GPS"
            }
            else{
                activityMainBinding.gpsStatus.text = "Disable GPS"
            }
        }

    }

    private fun updateBatteryLowStatus(){
        runOnUiThread {
            activityMainBinding.batteryStatus.text = "Battery Is Low"
        }

    }

    private fun updateMockingStatus(isMockingChanges:Boolean){
        runOnUiThread {
            if (isMockingChanges){
                activityMainBinding.mockingStatus.text = "Mocking Location"
            }
            else{
                activityMainBinding.mockingStatus.text = "Real Location"
            }
        }

    }


    private fun requestPermissions() {
//        PermissionManager.requestActivityRecognitionPermission(this)

        PermissionManager.requestPermissions(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
//            REQUEST_ACTIVITY_RECOGNITION_PERMISSION -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Log.d(TAG, "onRequestPermissionsResultddfds: ")
//                    requestLocationPermissions(this@MainActivity)
//                } else {
//                    // Permission denied, handle accordingly
//                }
//            }
            REQUEST_CODE_PERMISSIONS->{
                Log.d(TAG, "onRequestPermissionsResult: ")
                requestLocationPermissions(this@MainActivity)
            }
            REQUEST_LOCATION_PERMISSIONS -> {
                Log.d(TAG, "onRequestPermissionsResultddfds1: ")
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d(TAG, "onRequestPermissionsResultddfds2: ")
                    requestBackgroundLocationPermissions(this@MainActivity)
                } else {
                    // Permission denied, handle accordingly
                }
            }
            REQUEST_BACKGROUND_LOCATION_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All permissions granted, proceed with your logic
                    manageBatteryOptimization(this@MainActivity)
                } else {
                    // Permission denied, handle accordingly
                }
            }
        }
    }


    override fun onMockingFailed(error: String) {
        Log.d(TAG, "onMockingFailed: "+error)
        runOnUiThread {
            Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
            activityMainBinding.mockingSwitch.isChecked = false
        }
    }

    override fun onMockingStateDetected(isMocking: Boolean) {
        updateMockingStatus(isMocking)
    }


}