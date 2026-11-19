package com.nexo.hondalauncher

import android.app.Activity
import android.os.Bundle

/** Widget is activity ko kholta hai, ye screen lock karke foran band ho jati hai. */
class LockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LockHelper.lock(this)
        finish()
    }
}
