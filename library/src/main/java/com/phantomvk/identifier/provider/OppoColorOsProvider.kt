package com.phantomvk.identifier.provider

import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.phantomvk.identifier.model.ProviderConfig
import generated.com.oplus.stdid.IStdID

internal class OppoColorOsProvider(config: ProviderConfig) : AbstractProvider(config) {

  override fun isSupported(): Boolean {
    return isPackageInfoExisted("com.coloros.mcs")
  }

  override fun run() {
    val binderCallback = object : BinderCallback {
      override fun call(binder: IBinder): CallBinderResult {
        val asInterface = IStdID.Stub.asInterface(binder)
        if (asInterface == null) {
          return CallBinderResult.Failed(AIDL_INTERFACE_IS_NULL)
        }

        val sign = when (val result = getSignatureHash()) {
          is CallBinderResult.Failed -> return result
          is CallBinderResult.Success -> result.id
        }

        val id = asInterface.getSerID(config.context.packageName, sign, "OUID")
        return checkId(id)
      }
    }

    val component = ComponentName("com.coloros.mcs", "com.oplus.stdid.IdentifyService")
    val intent = Intent("action.com.oplus.stdid.ID_SERVICE").setComponent(component)
    bindService(intent, binderCallback)
  }
}