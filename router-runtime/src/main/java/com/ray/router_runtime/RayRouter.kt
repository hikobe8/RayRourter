package com.ray.router_runtime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log

class Route {

    private val startIntent = Intent()
    var targetActivityClass: String = ""
    var requestCode = -1
    var flags = 0

    fun requestCode(code: Int): Route {
        requestCode = code
        return this
    }

    fun withFlags(flag: Int): Route {
        flags = flag
        return this
    }

    fun putExtra(key: String, value: Any): Route {
        when (value) {
            is Boolean -> {
                startIntent.putExtra(key, value)
            }
            is Int -> {
                startIntent.putExtra(key, value)
            }
            is Long -> {
                startIntent.putExtra(key, value)
            }
            is Float -> {
                startIntent.putExtra(key, value)
            }
            is Double -> {
                startIntent.putExtra(key, value)
            }
            is String -> {
                startIntent.putExtra(key, value)
            }
            is Bundle -> {
                startIntent.putExtra(key, value)
            }
            is Parcelable -> {
                startIntent.putExtra(key, value)
            }
        }
        return this
    }

    fun navigation(context: Context) {
        try {
            val targetActivity = Class.forName(targetActivityClass)
            startIntent.setClass(context, targetActivity)
            if (flags > 0)
                startIntent.flags = flags
            if (requestCode > -1) {
                if (context is Activity) {
                    context.startActivityForResult(startIntent, requestCode)
                } else {
                    Log.e(
                        "RayRouter",
                        "$context can not call startActivityForResult() method with requestCode $requestCode"
                    )
                }
            } else {
                context.startActivity(startIntent)
            }
        } catch (e: Throwable) {
            Log.e("RayRouter", "error while start Activity = $targetActivityClass, e = $e")
        }
    }

}

object RayRouter {

    private const val TAG = "RayRouter"
    private const val CLASS_NAME = "com.ray.router.mapping.generated.RouterMapping"

    private val mapping = HashMap<String, String>()

    fun init() {
        try {
            val mappingClass = Class.forName(CLASS_NAME)
            val method = mappingClass.getMethod("get")
            val allMapping = method.invoke(null) as Map<String, String>
            if (allMapping.isNotEmpty()) {
                mapping.putAll(allMapping)
                Log.d(TAG, "init: get all mapping:")
                allMapping.onEach {
                    Log.d(TAG, "${it.key} : ${it.value}")
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, t.message!!)
        }
    }

    fun build(url: String): Route {
        val destUri = Uri.parse(url)
        val destScheme = destUri.scheme
        val destHost = destUri.host
        val destPath = destUri.path
        if (mapping.size < 1) {
            Log.e(TAG, "RouterMapping is empty!!! did you call RayRouter.init() method?")
        }
        val route = Route()
        mapping.onEach {
            val uri = Uri.parse(it.key)
            val scheme = uri.scheme
            val host = uri.host
            val path = uri.path
            if (destScheme == scheme && destHost == host && destPath == path) {
                route.targetActivityClass = it.value
                return@onEach
            }
        }
        if (TextUtils.isEmpty(route.targetActivityClass)) {
            Log.e(TAG, "error url = $url, no target Activity found!")
        }
        return route
    }

    fun inject(target: Any) {
        val targetActivityAutowired =
            Class.forName("${target::class.java.canonicalName}_Autowired")
        val injectMethod = targetActivityAutowired.getMethod("inject", Any::class.java)
        injectMethod.invoke(null, target)
    }

}