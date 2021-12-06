
## 配置工程

1. 在工程目录的build.grale添加jitpack仓库
    ```
    buildscript {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }

    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    ```

2. 在工程目录的build.grale添加router gradle插件
    ```
    buildscript {
        dependencies {
            ...
            classpath "com.github.hikobe8.RayRourter:router-gradle-plugin:0.2"
        }
    }
    ```

3. 添加依赖
    ```
    dependencies {
        ...
        //添加注解依赖库
        implementation 'com.github.hikobe8.RayRourter:router-annotations:0.2'
        //添加router运行时依赖
        implementation 'com.github.hikobe8.RayRourter:router-processor:0.2'
        //添加注解处理器
        kapt 'com.github.hikobe8.RayRourter:router-annotations:0.2'
    }
    ```

    **kapt依赖项不是可传递的，因此每个需要使用路由的module都要声明**


4. 配置路由文档生成路径(可选，如不需要可忽略)

    app的build.gradle中配置插件和文档路径
    ```
    apply plugin: 'com.ray.router'

    router {
        //这里使用的是工程根目录,可以自行配置
        wikiDir getProjectDir().absolutePath
    }
    ```

## 示例代码

* 一般使用

使用注解定义路由
```
@Destination(
        url = "router://test",
        description = "test page"
)
public class TestActivity extends AppCompatActivity {
    ...
}
```

跳转路由
```
class MaintActivity : AppCompatActivity() {
    ...
    fun jump(){
         RayRouter.build("router://test")
            .navigation(this)
    }
    ...
}
```

* 携带参数，自动解析绑定参数
  
使用注解定义路由, 在onCreate方法解析绑定参数
```
//Java
@Destination(
        url = "router://test",
        description = "test page"
)
public class TestActivity extends AppCompatActivity {

    @Autowired
    String argStr;

    @Autowired
    int argInt;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RayRouter.INSTANCE.inject(this);
        setContentView(R.layout.activity_test);
        TextView text = findViewById(R.id.text);
        text.setText(String.format("argStr = %s argInt = %d", argStr, argInt));
    }
}

//Kotlin
@Destination(
    url = "router://reading",
    description = "reading page"
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

```
添加参数跳转路由
```
class MainActivity : AppCompatActivity() {
    ...
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
    ...
}
```

## 实现原理
RayRouter的原理是使用了gradle插件生成路由表，然后使用ASM整合各个module中的路由表生成一个总的路由表实现跨module跳转，方便组件化架构的开发，然后使用JavaPoet生成每个路由页面参数自动解析绑定的帮助类文件，可以在声明注解处理器的module的build->generated->source->kapt文件中看到使用JavaPoet生成的解析参数的Java文件，文件名为Acitivity名_AutoWired.java。更多细节可以看源码，非常期待大佬们的建议和意见，谢谢！
