package com.ray.rayrouter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.ray.biz_reading.MyParcel
import com.ray.router.annotations.Destination
import com.ray.router_runtime.RayRouter

@Destination(
    url = "router://home",
    description = "应用主页"
)
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun clickTest(view: View) {
        RayRouter.build("router://test")
            .putExtra("argInt", 100)
            .putExtra("argStr", "clickTest")
            .navigation(this)
    }

    fun clickRead(view: View) {
        RayRouter.build("router://reading")
            .putExtra("argObj", MyParcel("reading page opened!"))
            .navigation(this)
    }
}