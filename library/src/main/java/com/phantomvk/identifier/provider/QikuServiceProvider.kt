package com.phantomvk.identifier.provider

import android.content.Intent
import android.os.IBinder
import com.phantomvk.identifier.model.ProviderConfig
import generated.com.qiku.id.IOAIDInterface

internal class QikuServiceProvider(config: ProviderConfig) : AbstractProvider(config) {

  override fun isSupported(): Boolean {
    return isPackageInfoExisted("com.qiku.id")
  }

  override fun run() {
    val binderCallback = object : BinderCallback {
      override fun call(binder: IBinder): CallBinderResult {
        val asInterface = IOAIDInterface.Stub.asInterface(binder)
        if (asInterface == null) {
          return CallBinderResult.Failed(AIDL_INTERFACE_IS_NULL)
        }

        if (config.isLimitAdTracking) {
          if (asInterface.isLimited) {
            return CallBinderResult.Failed(LIMIT_AD_TRACKING_IS_ENABLED)
          }
        }

        return checkId(asInterface.oaid)
      }
    }

    val intent = Intent("qiku.service.action.id")
    intent.setPackage("com.qiku.id")
    bindService(intent, binderCallback)
  }
}