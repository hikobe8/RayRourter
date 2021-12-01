package com.ray.rayrouter;

import com.ray.biz_reading.ReadingActivity;

/**
 * Author : Ray
 * Time : 2021/12/1 6:05 下午
 * Description :
 */
public class TestActivity$Autowired {

    public void inject(Object target){
        ReadingActivity realTarget = (ReadingActivity)target;
//        realTarget.argin = substitute.getIntent().getIntExtra("age", substitute.age);
//        realTarget.argStr = substitute.getIntent().getStringExtra("name");
//        realTarget.phone = (com.daddyno1.projectmoduledemo.bean.Phone) substitute.getIntent().getSerializableExtra("phone");
    }

}
