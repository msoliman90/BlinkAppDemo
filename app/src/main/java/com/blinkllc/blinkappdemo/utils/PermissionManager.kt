package com.blinkllc.blinkappdemo.utils

import android.Manifest
import android.Manifest.permission
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.pedant.SweetAlert.SweetAlertDialog
import com.blinkllc.blinkappdemo.R

object PermissionManager {
    private const val TAG = "PermissionManager"
    const val REQUEST_CODE_PERMISSIONS = 1000
    const val REQUEST_ACTIVITY_RECOGNITION_PERMISSION = 1001
    const val REQUEST_LOCATION_PERMISSIONS = 1002
    const val REQUEST_BACKGROUND_LOCATION_PERMISSIONS = 1003
    const val IGNORE_BATTERY_OPTIMIZATIONS_REQUEST_CODE = 1004


    fun requestActivityRecognitionPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            REQUEST_ACTIVITY_RECOGNITION_PERMISSION
        )
    }

    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            REQUEST_LOCATION_PERMISSIONS
        )
    }

    fun requestBackgroundLocationPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                REQUEST_BACKGROUND_LOCATION_PERMISSIONS
            )
        }
    }

    fun requestReadPhoneStatePermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_BACKGROUND_LOCATION_PERMISSIONS
            )
        }
    }

    fun promptEnableBackgroundLocation(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    private fun getPermissionsArray(): Array<String> {
        val permissions: ArrayList<String> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(permission.ACTIVITY_RECOGNITION)
            permissions.add(permission.READ_PHONE_STATE)
        }
        //this permission for make call incident
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(permission.CALL_PHONE)
            permissions.add(permission.PROCESS_OUTGOING_CALLS)
            permissions.add(permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(permission.POST_NOTIFICATIONS)
        }
        return permissions.toTypedArray()
    }
    fun requestPermissions(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity,
               getPermissionsArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }

    private fun requestBattery(activity: Activity) {
        val intent = Intent()
        val packageName = activity.packageName
        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
        intent.data = Uri.parse("package:$packageName")
        activity.startActivityForResult(
            intent,
           IGNORE_BATTERY_OPTIMIZATIONS_REQUEST_CODE
        )
    }
    fun manageBatteryOptimization(activity: Activity) {
        try {
            val packageName = activity.packageName
            val pm = activity.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                SweetAlertDialog(activity)
                    .setTitleText(activity.getString(R.string.battery_optimization))
                    .setContentText(activity.getString(R.string.battery_optimization_content))
                    .setConfirmButton(R.string.draw_overlay_ok) { dialog ->
                        dialog.dismissWithAnimation()
                       requestBattery(activity)
                    }
                    .setCancelButton(R.string.draw_overlay_cancel) { dialog ->
                        dialog.dismissWithAnimation()
                    }
                    .show()
            }
        } catch (e: Exception) {
            Log.d(
              TAG,
                "manageBatteryOptimizationError: " + e.message
            )
        }
    }

    fun isAccessFineAndCoarseLocationPermissionsGranted(context: Context): Boolean {
        return checkPermission(
            context,
           ACCESS_FINE_LOCATION,
            Build.VERSION_CODES.M
        ) &&
                checkPermission(
                    context,
                  ACCESS_COARSE_LOCATION,
                    Build.VERSION_CODES.M
                )   }

   private fun checkPermission(context: Context?, permission: String?, versionOfPermission: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= versionOfPermission) {
            ContextCompat.checkSelfPermission(
                context!!,
                permission!!
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}