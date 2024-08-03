package com.phantomvk.identifier.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.phantomvk.identifier.IdentifierManager
import com.phantomvk.identifier.interfaces.Disposable
import com.phantomvk.identifier.interfaces.OnResultListener
import com.phantomvk.identifier.model.ProviderConfig
import com.phantomvk.identifier.provider.AbstractProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.text.DecimalFormat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

  private var disposable: Disposable? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    findViewById<Button>(R.id.button).setOnClickListener { getId() }
    getId()
  }

  private fun getId() {
    val listener = object : OnResultListener {
      override fun onSuccess(id: String) { updateTextInfo(id) }
      override fun onError(msg: String, t: Throwable?) { updateTextInfo(msg, t) }
    }

    disposable?.dispose()
    disposable = IdentifierManager
      .getInstance()
      .setSubscriber(listener)
      .subscribe()
  }

  private fun updateTextInfo(msg: String? = null, t: Throwable? = null) {
    lifecycleScope.launch(Dispatchers.IO) {
      val deviceInfo = deviceInfo(if (t == null) msg ?: "" else "-")
      val str = getResultList().joinToString("\n\n") { "# ${it.tag}: (${it.ts} μs)\n${it.id}" }
      val finalStr = deviceInfo + "\n\n" + str
      Log.i("IdentifierTAG", finalStr, t)
      if (msg?.isNotBlank() == true) {
        copyToClipboard(finalStr)
      }

      val textView = findViewById<TextView>(R.id.system_textview)
      launch(Dispatchers.Main) { textView.text = finalStr }
    }
  }

  private fun deviceInfo(id: String): String {
    return """
        * Manufacturer: ${Build.MANUFACTURER}
        * Brand: ${Build.BRAND}
        * Model: ${Build.MODEL}
        * Device: ${Build.DEVICE}
        * Release: Android ${Build.VERSION.RELEASE} (SDK_INT: ${Build.VERSION.SDK_INT})
        * Display: ${Build.DISPLAY}
        * Incremental: ${Build.VERSION.INCREMENTAL}
        * Fingerprint: ${Build.FINGERPRINT}
        * | ${Build.MANUFACTURER} | ${Build.BRAND} | === | ${Build.MODEL} | ${Build.DEVICE} | ${Build.VERSION.SDK_INT} | ${Build.FINGERPRINT} |
        * AndroidId: ${getAndroidID(this)}
        * oaid: $id
      """.trimIndent()
  }

  private fun copyToClipboard(text: String) {
    try {
      val manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
      val clipData = ClipData.newPlainText("IdentifierDemo", text)
      manager.setPrimaryClip(clipData)
    } catch (ignore: Exception) {
    }
  }

  private fun getAndroidID(context: Context): String? {
    val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    return if (id == null || id == "9774d56d682e549c") null else id
  }

  private class ResultModel(val tag: String, val id: String, val ts: String? = null)

  private fun getResultList(): List<ResultModel> {
    val list = ArrayList<ResultModel>()
    val config = ProviderConfig(applicationContext).apply {
      isDebug = true
      isExperimental = true
      isLimitAdTracking = false
      isMemCacheEnabled = false
      executor = Executor { r -> Thread(r).start() }
      callback = WeakReference(object : OnResultListener {
        override fun onSuccess(id: String) {}
        override fun onError(msg: String, t: Throwable?) {}
      })
    }

    val clazz = Class.forName("com.phantomvk.identifier.impl.ManufacturerFactory")
    val instance = clazz.getField("INSTANCE").get(null)
    val providers = clazz.getMethod("getProviders", ProviderConfig::class.java)
      .apply { isAccessible = true }
      .invoke(instance, config) as List<AbstractProvider>

    val decimalFormat = DecimalFormat("#,###")
    for (provider in providers) {
      val startNameTs = System.nanoTime()
      val isSupported = try {
        provider.isSupported()
      } catch (t: Throwable) {
        false
      }

      if (!isSupported) {
        continue
      }

      val latch = CountDownLatch(1)
      val resultCallback = object : OnResultListener {
        override fun onSuccess(id: String) {
          val consumed = (System.nanoTime() - startNameTs) / 1000L
          val formatTs = decimalFormat.format(consumed)
          list.add(ResultModel(provider.getTag(), id, formatTs))
          latch.countDown()
        }

        override fun onError(msg: String, t: Throwable?) {
          list.add(ResultModel(provider.getTag(), msg))
          latch.countDown()
        }
      }
      provider.setCallback(resultCallback)
      provider.run()
      latch.await()
    }

    return list
  }

  override fun onDestroy() {
    super.onDestroy()
    disposable?.dispose()
  }
}