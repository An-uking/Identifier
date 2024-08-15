package com.phantomvk.identifier.provider

import android.content.Intent
import android.os.IBinder
import com.phantomvk.identifier.model.ProviderConfig
import generated.com.samsung.android.deviceidservice.IDeviceIdService

internal class SamsungProvider(config: ProviderConfig) : AbstractProvider(config) {

  private val pkgName = "com.samsung.android.deviceidservice"
  private val className = "com.samsung.android.deviceidservice.DeviceIdService"

  override fun isSupported(): Boolean {
    return isPackageInfoExisted(pkgName)
  }

  override fun run() {
    val binderCallback = object : BinderCallback {
      override fun call(binder: IBinder): CallBinderResult {
        val asInterface = IDeviceIdService.Stub.asInterface(binder)
        if (asInterface == null) {
          return CallBinderResult.Failed(AIDL_INTERFACE_IS_NULL)
        }

        return checkId(asInterface.oaid)
      }
    }

    val intent = Intent().setClassName(pkgName, className)
    bindService(intent, binderCallback)
  }
}