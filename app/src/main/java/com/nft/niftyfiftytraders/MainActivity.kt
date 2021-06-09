package com.nft.niftyfiftytraders

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader


class MainActivity : AppCompatActivity() {

    private lateinit var myWebView: WebView
    private lateinit var helper: Helper
    private var mInterstitialAd: InterstitialAd? = null
    private val homePage = "https://niftyfiftytraders.com/auth/user/login"

    @SuppressLint("JavascriptInterface", "HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var adRequest = AdRequest.Builder().build()

//        "ca-app-pub-5988912071818769/4415275939"
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })

        // generate FCM token
        if (getString() == "null") {
            getFirebaseToken()
        }

        myWebView = findViewById(R.id.webview)
        myWebView.settings.javaScriptEnabled = true
        myWebView.addJavascriptInterface(WebAppInterface(this), "Android")
        myWebView.webViewClient = WebViewClient()
        myWebView.loadUrl(homePage)

    }

    private fun getFirebaseToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            if (token != null) {
                sendToServer(token)
            }
        })
        Firebase.messaging.isAutoInitEnabled = true
    }

    private fun sendToServer(token: String) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://192.168.0.136/curl/index.php?token=$token&android_id=1234"
        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                Log.d(TAG, "Response is: ${response}")
                storeString(token)
            },
            Response.ErrorListener { Log.d(TAG, "Response is: Error") })
        queue.add(stringRequest)
    }

    class WebAppInterface(private val mContext: Context) {
        /** Show a toast from the web page  */
        @JavascriptInterface
        fun showToast(toast: String) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
        }
    }


    private class MyWebViewClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

            Log.d(TAG, "loaded url " + url)
            if (Uri.parse(url).host == "www.example.com") {
                // This is my web site, so do not override; let my WebView load the page
                return false
            }
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                startActivity(this)
            }
            return true
        }

        private fun startActivity(intent: Intent) {

        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Check if the key event was the Back button and if there's history
        if (keyCode == KeyEvent.KEYCODE_BACK && myWebView.canGoBack()) {
            myWebView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val TAG = "MyActivity"
    }

    private fun storeString(data: String) {
        val file: String = "fcmtoken.txt"
        val data: String = data
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = openFileOutput(file, Context.MODE_PRIVATE)
            fileOutputStream.write(data.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getString(): String {

        return try {
            var fileInputStream: FileInputStream? = null
            fileInputStream = openFileInput("fcmtoken.txt")
            var inputStreamReader: InputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader: BufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String? = null
            while ({ text = bufferedReader.readLine(); text }() != null) {
                stringBuilder.append(text)
            }
            stringBuilder.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "null"
        }

    }
}


