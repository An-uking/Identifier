package com.phantomvk.identifier.provider

import android.app.KeyguardManager
import android.content.Context.KEYGUARD_SERVICE
import com.phantomvk.identifier.model.ProviderConfig

class CooseaProvider(config: ProviderConfig) : AbstractProvider(config) {

  private val manager = config.context.getSystemService(KEYGUARD_SERVICE) as? KeyguardManager

  override fun getTag(): String {
    return "CooseaProvider"
  }

  override fun ifSupported(): Boolean {
    if (manager == null) return false
    val method = manager::class.java.getDeclaredMethod("isSupported")
    return (method.invoke(manager) as? Boolean) == true
  }

  override fun execute() {
    val method = manager!!::class.java.getDeclaredMethod("obtainOaid")
    val id = method.invoke(manager) as String?
    checkId(id, getCallback())
  }
}