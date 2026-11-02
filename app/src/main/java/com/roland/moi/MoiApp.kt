package com.roland.moi

import android.app.Application
import com.roland.moi.backup.BackupWorker
import com.roland.moi.data.Store
import com.roland.moi.notify.Reminders

class MoiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Reminders.createChannel(this)
        Reminders.scheduleAll(this)
        BackupWorker.schedulePeriodic(this)
    }
}
