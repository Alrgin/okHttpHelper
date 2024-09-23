package com.example.okhttphelper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class EventBusLifecycleObserver(private val subscriber: Any) : LifecycleObserver {
    //监听onStart方法
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        //注册EventBus
        EventBusHelper.register(subscriber)
    }
    //监听onStop方法
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        //解注册EventBus
        EventBusHelper.unregister(subscriber)
    }
}
