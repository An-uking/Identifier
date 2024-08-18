package com.phantomvk.identifier.provider

import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import com.phantomvk.identifier.model.ProviderConfig
import generated.com.hihonor.cloudservice.oaid.IOAIDCallBack
import generated.com.hihonor.cloudservice.oaid.IOAIDService
import java.util.concurrent.CountDownLatch

internal class HonorServiceProvider(config: ProviderConfig) : AbstractProvider(config) {

  override fun isSupported(): Boolean {
    return isPackageInfoExisted("com.hihonor.id")
  }

  override fun run() {
    val binderCallback = object : BinderCallback {
      override fun call(binder: IBinder): CallBinderResult {
        val asInterface = IOAIDService.Stub.asInterface(binder)
        if (asInterface == null) {
          return CallBinderResult.Failed(AIDL_INTERFACE_IS_NULL)
        }

        if (config.isLimitAdTracking) {
          val result = isLimited(asInterface)
          if (result != null) {
            return result
          }
        }

        return getId(asInterface) ?: CallBinderResult.Failed(ID_INFO_IS_NULL)
      }
    }

    val intent = Intent("com.hihonor.id.HnOaIdService")
    intent.setPackage("com.hihonor.id")
    bindService(intent, binderCallback)
  }

  private fun isLimited(asInterface: IOAIDService): CallBinderResult? {
    var result: CallBinderResult? = null
    val latch = CountDownLatch(1)
    asInterface.isLimited(object : IOAIDCallBack.Stub() {
      override fun a(i: Int, j: Long, z: Boolean, f: Float, d: Double, str: String?) {}
      override fun onResult(i: Int, bundle: Bundle?) {
        if (i == 0 && bundle?.getBoolean("oa_id_limit_state") == true) {
          result = CallBinderResult.Failed(LIMIT_AD_TRACKING_IS_ENABLED)
        }
        latch.countDown()
      }
    })

    latch.await()
    return result
  }

  private fun getId(asInterface: IOAIDService): CallBinderResult? {
    var result: CallBinderResult? = null
    val latch = CountDownLatch(1)
    asInterface.getOaid(object : IOAIDCallBack.Stub() {
      override fun a(i: Int, j: Long, z: Boolean, f: Float, d: Double, str: String?) {}
      override fun onResult(i: Int, bundle: Bundle?) {
        if (i != 0 || bundle == null) {
          result = CallBinderResult.Failed(BUNDLE_IS_NULL)
          latch.countDown()
          return
        }

        result = checkId(bundle.getString("oa_id_flag"))
        latch.countDown()
      }
    })

    latch.await()
    return result
  }
}
