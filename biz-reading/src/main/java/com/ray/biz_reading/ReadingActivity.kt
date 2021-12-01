package com.ray.biz_reading

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ray.router.annotations.Autowired
import com.ray.router.annotations.Destination

@Destination(
    url = "router://reading",
    description = "阅读页"
)
class ReadingActivity : AppCompatActivity() {

    @Autowired
    var argStr: String? = null

    @Autowired
    public var argInt = 0

    @Autowired
    var argObj: Any? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reading)
    }
}