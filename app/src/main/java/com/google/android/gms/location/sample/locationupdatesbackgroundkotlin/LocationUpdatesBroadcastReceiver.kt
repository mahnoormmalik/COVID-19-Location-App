/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.location.sample.locationupdatesbackgroundkotlin

import android.app.ActivityManager
import android.app.PendingIntent.getActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.data.LocationRepository
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.data.db.MyLocationEntity
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.ui.MainActivity
import java.util.concurrent.Executors
import java.net.HttpURLConnection
import java.net.URL

import com.loopj.android.http.AsyncHttpResponseHandler

import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.RequestParams

import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.*
import android.provider.Settings
import java.security.AccessController.getContext
import com.google.android.gms.location.LocationAvailability



private const val TAG = "LUBroadcastReceiver"

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O and above
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates in the background. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should NOT be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
class LocationUpdatesBroadcastReceiver : BroadcastReceiver() {
    private val url = "http://172.30.20.170:5000" //****Put your  URL here******
    private val POST = "POST"
    private val GET = "GET"
    var id = -1;
    var android_id= "0"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")
        android_id = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        Log.d(TAG, "Android ID: $android_id")
        if (intent.action == ACTION_PROCESS_UPDATES) {
            // Checks for location availability changes.

            LocationAvailability.extractLocationAvailability(intent)?.let { locationAvailability ->
                if (!locationAvailability.isLocationAvailable) {
                    Log.d(TAG, "Location services are no longer available!")
                }
            }

            LocationResult.extractResult(intent)?.let { locationResult ->
                val locations = locationResult.locations.map { location ->
                    MyLocationEntity(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        foreground = isAppInForeground(context),
                        date = Date(location.time)
                    )
                }
                if (locations.isNotEmpty()) {
                    LocationRepository.getInstance(context, Executors.newSingleThreadExecutor())
                        .addLocations(locations)
                    for (location in locations) {
                        var paramname = arrayOf("uniqueID","lat", "lng", "date")
                        var params = arrayOf(android_id, location.latitude.toString(), location.longitude.toString(), location.date.toString())
                        sendRequest(POST, "locationUpdate", paramname, params)
                    }
                }
            }
        }
    }
    fun get_user_id(): Int {
//        Log.d("Accounts: ", accounts.toString())
//        if (accounts.isNotEmpty()){

        var paramname = arrayOf("uniqueID")
        var params = arrayOf(android_id)
        sendRequest("POST", "getUserID", paramname, params )
//        }
//        else{
//            sendRequest("POST", "getUserID", "accounts", accounts.toString() )
//        }
        return id;

    }

    private fun sendRequest(type: String, method: String, paramname: Array<String>, param: Array<String>) {

        /* if url is of our get request, it should not have parameters according to our implementation.
        * But our post request should have 'name' parameter. */


        val client = AsyncHttpClient()
        val params = RequestParams()
        for (i in paramname.indices) {
            params.put(paramname[i], param[i])

        }
        Log.d(TAG, "Params: $params")
        Log.d(TAG,id.toString())
        client.post("$url/$method", params, object : AsyncHttpResponseHandler(){
            override fun onStart() {
                // called before request is started
                Log.d(TAG, "Sending Http request")
            }

            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header?>?,
                response: ByteArray
            ) {
                // called when response HTTP status is "200 OK"
                Log.d(TAG, "HTTP response 200: " + String(response))
                var res = String(response)
                Log.d(TAG, "HTTP response 200: 1 $res")
                var obj = JSONObject(res.substring(res.indexOf("{"), res.lastIndexOf("}") + 1))
//                Log.d(TAG, "HTTP response 200: id$id")
                if(obj.has("id")){
                    id = obj.getInt("id")

                }

                Log.d(TAG, "HTTP response 200: " + String(response))
            }

            override fun onFailure(
                statusCode: Int,
                headers: Array<Header?>?,
                errorResponse: ByteArray?,
                e: Throwable?
            ) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
                Log.d(TAG, "HTTP response not good")
            }

            override fun onRetry(retryNo: Int) {
                // called when request is retried
            }
        })
//        conn.requestMethod = type
//        conn.doOutput = true
//        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
//        conn.setRequestProperty("Content-Length", postData.length.toString())
//        conn.useCaches = false
//
//        DataOutputStream(conn.outputStream).use { it.writeBytes(postData) }
//        BufferedReader(InputStreamReader(conn.inputStream)).use { br ->
//            var line: String?
//            while (br.readLine().also { line = it } != null) {
//                println(line)
//            }
//        }
    }

    // Note: This function's implementation is only for debugging purposes. If you are going to do
    // this in a production app, you should instead track the state of all your activities in a
    // process via android.app.Application.ActivityLifecycleCallbacks's
    // unregisterActivityLifecycleCallbacks(). For more information, check out the link:
    // https://developer.android.com/reference/android/app/Application.html#unregisterActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks
    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false

        appProcesses.forEach { appProcess ->
            if (appProcess.importance ==
                ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                appProcess.processName == context.packageName) {
                return true
            }
        }
        return false
    }

    companion object {
        const val ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.action." +
                    "PROCESS_UPDATES"
    }
}
