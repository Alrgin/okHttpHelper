package com.example.okhttphelper
import com.google.gson.Gson
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.GzipSink
import okio.Okio
import okio.buffer
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlin.jvm.Throws

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

    /**
     * Gzip请求体内部类，用于构建被Gzip压缩的Body
     */
    class GzipRequestBody(val requestBody: RequestBody) : RequestBody(){
        override fun contentType(): MediaType? {
            //返回原Body的MiMe
            return requestBody.contentType()
        }
        override fun writeTo(sink: BufferedSink) {
            //GzipSink压缩流用于压缩数据
            val gzipSink = GzipSink(sink).buffer()
            //将Body给写入GzipSink压缩
            requestBody.writeTo(gzipSink)
            //关闭压缩流
            gzipSink.close()
        }

    }
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
     * @param requestBodyObj:请求体的类型
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
    /**@param url : 请求的Url
     * @param requestBodyObj : 请求体的类型
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
    **/
    // 异步 PUT 请求
    fun <T, R> put
    (url: String,
     requestBodyObj: T,
     responseType: Type,
     onSuccess: (R) -> Unit,
     onFailure: (String) -> Unit) {
        val json = gson.toJson(requestBodyObj)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
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
    /**
     * @param url : 请求的Url
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
     */
    // 异步 DELETE 请求
    fun <T> delete
    (url: String,
     responseType: Type,
     onSuccess: (T) -> Unit,
     onFailure: (String) -> Unit) {
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
    /**
     * @param description : 文件的描述
     * @param url : 请求的url
     * @param file : 文件
     * @param responseType : 返回值的类型
     * @param onSuccess : 成功回调
     * @param onFailure : 失败回调
     */
    // 上传 Gzip 压缩后的文件，并附加一个字符串参数
    fun<T> uploadGzipFile
     (description:String,
      url: String,
      file: File,
      responseType: Type,
      onSuccess: (T) -> Unit,
      onFailure: (String) -> Unit) {
        // 创建一个 RequestBody，包装原始文件的数据
        val fileBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        // 使用 GzipRequestBody 包装原始的 RequestBody，确保文件在上传时进行 Gzip 压缩
        val gzipBody = GzipRequestBody(fileBody)
        // 构建 MultipartBody，用于上传文件、字符串参数和其他表单字段
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM) // 设置表单类型
            .addFormDataPart("file", file.name, gzipBody) // 添加文件字段，文件将以 Gzip 压缩的形式上传
            .addFormDataPart("description", description) // 添加文件描述
            .build()
        // 构建 HTTP 请求，指定目标 URL 和请求体
        val request = Request.Builder()
            .url(url) // 指定上传文件的服务器 URL
            .post(requestBody) // 使用 POST 方法上传数据
            .build()
        // 使用 OkHttpClient 异步发送请求，避免阻塞主线程
        client.newCall(request).enqueue(object : Callback {
            // 处理请求失败的情况
            override fun onFailure(call: Call, e: IOException) {
                onFailure(e.message ?: "Unknown Error") // 调用失败回调并传递错误信息
            }
            // 处理服务器的响应
            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    // 将响应体转换为字符串
                    val json = responseBody.string()
                    try {
                        // 解析 JSON 响应到指定的泛型类型 T
                        val parsedObject: T = gson.fromJson(json, responseType)
                        // 成功时，调用 onSuccess 回调并传递解析后的对象
                        onSuccess(parsedObject)
                    } catch (e: Exception) {
                        // 如果解析失败，调用 onFailure 回调并传递错误信息
                        onFailure("Failed to parse response: ${e.message}")
                    }
                } ?: onFailure("Response body is null") // 如果响应体为空，调用 onFailure 回调
            }
        })
    }
    /**
     * @param url : WebSocket 服务器的 URL
     * @param onOpen : WebSocket 连接成功回调
     * @param onMessage : 收到消息时的回调
     * @param onFailure : 连接失败或发生错误时的回调
     * @param onClosing : 连接关闭时的回调
     * @param onClosed : 连接完全关闭时的回调
     */
    fun connectWebSocket(
        url: String,
        onOpen: (WebSocket) -> Unit,
        onMessage: (String) -> Unit,
        onFailure: (String) -> Unit,
        onClosing: (Int, String) -> Unit,
        onClosed: (Int, String) -> Unit
    ) {
        // 构建 WebSocket 请求
        val request = Request.Builder()
            .url(url)
            .build()
        // 创建 WebSocket 并添加事件监听器
        client.newWebSocket(request, object : WebSocketListener() {
            // WebSocket 连接成功
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onOpen(webSocket) // 连接成功时回调
            }

            // 收到服务器发送的消息
            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessage(text) // 收到消息时回调
            }

            // WebSocket 连接出错
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onFailure(t.message ?: "Unknown Error") // 出错时回调
            }

            // WebSocket 连接关闭的处理
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onClosing(code, reason) // 连接关闭时回调
                webSocket.close(code, reason) // 主动关闭 WebSocket 连接
            }

            // WebSocket 连接已关闭
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed(code, reason) // 连接完全关闭时回调
            }
        })
    }
    /**
     * 关闭 WebSocket
     * @param webSocket : WebSocket 对象
     * @param code : 关闭码
     * @param reason : 关闭原因
     */
    fun closeWebSocket(webSocket: WebSocket, code: Int = 1000, reason: String = "Normal closure") {
        webSocket.close(code, reason)
    }
}
