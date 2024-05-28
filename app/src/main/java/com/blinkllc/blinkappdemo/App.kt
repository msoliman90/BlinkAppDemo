//package com.blinkllc.blinkappdemo
//
//import android.content.Context
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleEventObserver
//import androidx.lifecycle.LifecycleOwner
//import androidx.multidex.MultiDex
//import androidx.multidex.MultiDexApplication
//
//class App : MultiDexApplication(), LifecycleEventObserver {
//    override fun onCreate() {
//        super.onCreate()
//        MultiDex.install(this)
//
//
//
//
//        //FirebaseApp.initializeApp(this);
//
////        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
////        if (StringUtils.isValidString(refreshedToken))
////            DataManager.getInstance().sendClientToken(refreshedToken);
//        //TODO uncomment line below after adding fabric api secret and key to fabric.properties
////        Fabric.with(this, new Crashlytics());
//        //Crashlytics.getInstance().crash();
//    }
//
//    //TODO uncomment line below if you need to add multidex support and extend App class from MultiDexApplication
////    override fun attachBaseContext(base: Context) {
////
////    }
//
//
//    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
//
//    }
//}