package com.roland.moi

import android.app.Application
import com.roland.moi.backup.BackupWorker
import com.roland.moi.data.Store

class MoiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        BackupWorker.schedulePeriodic(this)
    }
}
