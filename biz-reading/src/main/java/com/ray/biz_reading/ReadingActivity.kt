package com.ray.biz_reading

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.ray.router.annotations.Autowired
import com.ray.router.annotations.Destination
import com.ray.router_runtime.RayRouter

@Destination(
    url = "router://reading",
    description = "阅读页"
)
class ReadingActivity : AppCompatActivity() {

    @JvmField
    @Autowired(required = true)
    var argObj: MyParcel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RayRouter.inject(this)
        setContentView(R.layout.activity_reading)
        val text = findViewById<TextView>(R.id.tv)
        text.text = argObj?.msg
    }
}