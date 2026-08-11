package com.aatorque.speedometer.androidauto

import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService
import timber.log.Timber

class AutoService : CarActivityService() {

    override fun onCreate() {
        super.onCreate()
        Timber.tag("SpeedometerAuto").i("AutoService (CarActivityService) criado")
    }

    override fun getCarActivity(): Class<out CarActivity> {
        Timber.tag("SpeedometerAuto").i("Retornando AutoSpeedometerActivity.class para o Android Auto")
        return AutoSpeedometerActivity::class.java
    }
}
