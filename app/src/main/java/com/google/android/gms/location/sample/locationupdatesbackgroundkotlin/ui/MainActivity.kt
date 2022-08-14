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
package com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.R
import com.google.android.gms.location.sample.locationupdatesbackgroundkotlin.databinding.ActivityMainBinding
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.RequestParams
import cz.msebera.android.httpclient.Header
import org.json.JSONObject

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

import java.util.*

/**
 * This app allows a user to receive location updates in the background.
 *
 * Users have four options in Android 11+ regarding location:
 *
 *  * One time only
 *  * Allow while app is in use, i.e., while app is in foreground
 *  * Allow all the time
 *  * Not allow location at all
 *
 * IMPORTANT NOTE: You should generally prefer 'while-in-use' for location updates, i.e., receiving
 * location updates while the app is in use and create a foreground service (tied to a Notification)
 * when the user navigates away from the app. To learn how to do that instead, review the
 * @see <a href="https://codelabs.developers.google.com/codelabs/while-in-use-location/index.html?index=..%2F..index#0">
 * Receive location updates in Android 10 with Kotlin</a> codelab.
 *
 * If you do have an approved use case for receiving location updates in the background, it will
 * require an additional permission (android.permission.ACCESS_BACKGROUND_LOCATION).
 *
 *
 * Best practices require you to spread out your first fine/course request and your background
 * request.
 */
class MainActivity : AppCompatActivity(), PermissionRequestFragment.Callbacks,
    LocationUpdateFragment.Callbacks {

    private val TAG = "MainActivityPost"

    private val url = "http://172.30.20.170:5000"
    var uniqueID = UUID.randomUUID().toString()
//    var id = get_user_id();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val am: AccountManager = AccountManager.get(this) // "this" references the current Context
        val accounts: Array<out Account> = am.getAccountsByType("com.google")
        setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

//        if (id == -1){
//            id = get_user_id()
//        }

        if (currentFragment == null) {

            val fragment = LocationUpdateFragment.newInstance()
//            val bundle = Bundle().apply{
//                putInt("id", id)
//            }
//            fragment.arguments = bundle
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
        }
    }

    // Triggered from the permission Fragment that it's the app has permissions to display the
    // location fragment.
    override fun displayLocationUI() {

        val fragment = LocationUpdateFragment.newInstance()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Triggers a splash screen (fragment) to help users decide if they want to approve the missing
    // fine location permission.
    override fun requestFineLocationPermission() {
        val fragment = PermissionRequestFragment.newInstance(PermissionRequestType.FINE_LOCATION)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Triggers a splash screen (fragment) to help users decide if they want to approve the missing
    // background location permission.
    override fun requestBackgroundLocationPermission() {
        val fragment = PermissionRequestFragment.newInstance(
            PermissionRequestType.BACKGROUND_LOCATION
        )

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

//    fun getMyID() : Int {
//        return id
//    }
//    fun get_user_id(): Int {
////        Log.d("Accounts: ", accounts.toString())
////        if (accounts.isNotEmpty()){
//            var uniqueID = UUID.randomUUID().toString()
//            sendRequest("POST", "getUserID", "uniqueID", uniqueID )
////        }
////        else{
////            sendRequest("POST", "getUserID", "accounts", accounts.toString() )
////        }
//        return id;
//
//    }
    private fun sendRequest(type: String, method: String, paramname: String?, param: String?)  {

        /* if url is of our get request, it should not have parameters according to our implementation.
        * But our post request should have 'name' parameter. */

        // Asynchronous client
        val client = AsyncHttpClient()
        val params = RequestParams(paramname, param)
        client.post("$url/$method", params, object : AsyncHttpResponseHandler() {
            override fun onStart() {
                // called before request is started
                Log.d(TAG, "Sending Http request")
            }

            override fun onSuccess(
                statusCode: Int,
                headers: Array<Header?>?,
                response: ByteArray?
            ) {
                // called when response HTTP status is "200 OK"
                if (response != null){
                    Log.d(TAG, "HTTP response 200: " + String(response))
                    var res = String(response)
                    Log.d(TAG, "HTTP response 200: 1 $res")
                    var obj = JSONObject(res.substring(res.indexOf("{"), res.lastIndexOf("}") + 1))
//                    id = obj.getInt("id")
//                    Log.d(TAG, "HTTP response 200: id$id")

                }
                else{
                    Log.d(TAG, "HTTP response 200")

                }
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
    }
}
