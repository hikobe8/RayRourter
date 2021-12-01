package com.ray.rayrouter;

import android.content.Intent;

import com.ray.biz_reading.ReadingActivity;

/**
 * Author : Ray
 * Time : 2021/12/1 6:05 下午
 * Description :
 */
public class TestActivity$Autowired {

    public void inject(Object target) {
        ReadingActivity realTarget = (ReadingActivity) target;
        Intent realTargetIntent = realTarget.getIntent();
        realTarget.argInt = realTargetIntent.getIntExtra("argInt", -1);
        realTarget.argStr = realTargetIntent.getStringExtra("argStr");
        realTarget.argObj = realTargetIntent.getParcelableExtra("argObj");
    }

}
