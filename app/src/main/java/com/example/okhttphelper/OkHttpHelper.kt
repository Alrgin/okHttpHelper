package com.example.okhttphelper
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object OkHttpHelper {
    //懒加载OkhttpClient
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    //解析返回的Json和发送的Json
    private val gson: Gson by lazy { Gson() }
    // 异步 GET 请求
    /**
     * @param url : 请求的Url
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
     * 泛型在定义了responseType后可不写
     */
    fun <T> get(url: String, responseType: Type, onSuccess: (T) -> Unit, onFailure: (String) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // 请求失败，回调 onFailure
                onFailure(e.message ?: "Unknown Error")
            }
            override fun onResponse(call: Call, response: Response) {
                // 请求成功，处理响应
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    val parsedObject: T = gson.fromJson(json, responseType)
                    onSuccess(parsedObject)
                } ?: onFailure("Response body is null")
            }
        })
    }
    // 异步 POST 请求
    /**
     * @param url : 请求的Url
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
     * 泛型在定义了responseType后可不写
     */
    fun <T, R> post(url: String, requestBodyObj: T, responseType: Type, onSuccess: (R) -> Unit, onFailure: (String) -> Unit) {
        val json = gson.toJson(requestBodyObj)
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Unknown Error")
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    //将返回的值转换成对应的类型
                    val parsedObject: R = gson.fromJson(json, responseType)
                    //回调
                    onSuccess(parsedObject)
                } ?: onFailure("Response body is null")
            }
        })
    }
    /**
     * @param url : 请求的Url
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
     * @param queryParams : 携带的参数
     * 泛型在定义了responseType后可不写
     */
    fun <T> get(
        url: String,
        queryParams: Map<String, String> = emptyMap(), // 允许传递查询参数
        responseType: Type,
        onSuccess: (T) -> Unit,
        onFailure: (String) -> Unit
    ) {
        // 使用 HttpUrl.Builder 动态添加查询参数
        val httpUrlBuilder = url.toHttpUrlOrNull()?.newBuilder() ?: run {
            onFailure("Invalid URL")
            return
        }
        // 将查询参数加入到 URL 中
        for ((key, value) in queryParams) {
            httpUrlBuilder.addQueryParameter(key, value)
        }
        val request = Request.Builder()
            .url(httpUrlBuilder.build()) // 使用包含查询参数的 URL
            .get()
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Unknown Error")
            }
            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    val parsedObject: T = gson.fromJson(json, responseType)
                    onSuccess(parsedObject)
                } ?: onFailure("Response body is null")
            }
        })
    }
    // 异步 PUT 请求
    fun <T, R> put(url: String, requestBodyObj: T, responseType: Type, onSuccess: (R) -> Unit, onFailure: (String) -> Unit) {
        val json = gson.toJson(requestBodyObj)
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Unknown Error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    val parsedObject: R = gson.fromJson(json, responseType)
                    onSuccess(parsedObject)
                } ?: onFailure("Response body is null")
            }
        })
    }
    // 异步 DELETE 请求
    fun <T> delete(url: String, responseType: Type, onSuccess: (T) -> Unit, onFailure: (String) -> Unit) {
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Unknown Error")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val json = responseBody.string()
                    val parsedObject: T = gson.fromJson(json, responseType)
                    onSuccess(parsedObject)
                } ?: onFailure("Response body is null")
            }
        })
    }
}
