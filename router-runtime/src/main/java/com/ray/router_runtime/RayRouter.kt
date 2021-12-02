package com.ray.router_runtime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.Log

object RayRouter {

    private const val TAG = "RayRouter"
    private const val CLASS_NAME = "com.ray.router.mapping.generated.RouterMapping"

    private val mapping = HashMap<String, String>()

    private var targetActivityClass = ""
    private var requestCode = -1
    private var startIntent: Intent? = null
    private var flags = 0

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

    fun build(url: String): RayRouter {
        startIntent = Intent()
        val destUri = Uri.parse(url)
        val destScheme = destUri.scheme
        val destHost = destUri.host
        val destPath = destUri.path
        if (mapping.size < 1) {
            Log.e(TAG, "RouterMapping is empty!!! did you call RayRouter.init() method?")
        }
        mapping.onEach {
            val uri = Uri.parse(it.key)
            val scheme = uri.scheme
            val host = uri.host
            val path = uri.path
            if (destScheme == scheme && destHost == host && destPath == path) {
                targetActivityClass = it.value
                return@onEach
            }
        }
        if (targetActivityClass.isEmpty()) {
            Log.e(TAG, "error url = $url, no target Activity found!")
        }
        return this
    }

    fun requestCode(code: Int): RayRouter {
        requestCode = code
        return this
    }

    fun withFlags(flag: Int): RayRouter {
        flags = flag
        return this
    }

    fun putExtra(key: String, value: Any): RayRouter {
        when (value) {
            is Boolean -> {
                startIntent?.putExtra(key, value)
            }
            is Int -> {
                startIntent?.putExtra(key, value)
            }
            is Long -> {
                startIntent?.putExtra(key, value)
            }
            is Float -> {
                startIntent?.putExtra(key, value)
            }
            is Double -> {
                startIntent?.putExtra(key, value)
            }
            is String -> {
                startIntent?.putExtra(key, value)
            }
            is Bundle -> {
                startIntent?.putExtra(key, value)
            }
            is Parcelable -> {
                startIntent?.putExtra(key, value)
            }
        }
        return this
    }

    fun navigation(context: Context) {
        try {
            val targetActivity = Class.forName(targetActivityClass)
            if (null == startIntent)
                startIntent = Intent()
            startIntent?.setClass(context, targetActivity)
            if (flags > 0)
                startIntent?.setFlags(flags)
            if (requestCode > -1) {
                if (context is Activity) {
                    context.startActivityForResult(startIntent, requestCode)
                } else {
                    Log.e(
                        TAG,
                        "$context can not call startActivityForResult() method with requestCode $requestCode"
                    )
                }
            } else {
                context.startActivity(startIntent)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "error while start Activity = $targetActivityClass, e = $e")
        }
    }

    fun inject(target: Any) {
        val targetActivityAutowired =
            Class.forName("${target::class.java.canonicalName}_AutoWired")
        val injectMethod = targetActivityAutowired.getMethod("inject", Any::class.java)
        injectMethod.invoke(null, target)
    }

}