package com.phantomvk.identifier.provider

import android.content.Intent
import android.os.IBinder
import com.phantomvk.identifier.interfaces.BinderCallback
import com.phantomvk.identifier.model.CallBinderResult
import com.phantomvk.identifier.model.ProviderConfig
import generated.com.android.creator.IdsSupplier

internal class FreemeProvider(config: ProviderConfig) : AbstractProvider(config) {

  override fun getTag(): String {
    return "FreemeProvider"
  }

  override fun isSupported(): Boolean {
    return isPackageInfoExisted("com.android.creator")
  }

  override fun run() {
    val binderCallback = object : BinderCallback {
      override fun call(binder: IBinder): CallBinderResult {
        val asInterface = IdsSupplier.Stub.asInterface(binder)
        if (asInterface == null) {
          return CallBinderResult.Failed(AIDL_INTERFACE_IS_NULL)
        }

        if (config.isLimitAdTracking) {
          val isSupported = asInterface.isSupported
          if (!isSupported) {
            return CallBinderResult.Failed(LIMIT_AD_TRACKING_IS_ENABLED)
          }
        }

        val id = asInterface.oaid
        return checkId(id)
      }
    }

    val intent = Intent("android.service.action.msa")
    intent.setPackage("com.android.creator")
    bindService(intent, binderCallback)
  }
}