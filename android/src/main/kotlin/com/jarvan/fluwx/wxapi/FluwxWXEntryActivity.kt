/*
 * Copyright (C) 2020 The OpenFlutter Organization
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jarvan.fluwx.wxapi

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import com.jarvan.fluwx.handlers.FluwxResponseHandler
import com.jarvan.fluwx.handlers.FluwxRequestHandler
import com.jarvan.fluwx.handlers.WXAPiHandler
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import io.flutter.Log


open class FluwxWXEntryActivity : Activity(), IWXAPIEventHandler {

    // IWXAPI 是第三方app和微信通信的openapi接口

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isTaskRoot) {
            finish();
        }
        try {
            if (!WXAPiHandler.wxApiRegistered) {
                val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                val wechatAppId = appInfo.metaData.getString("weChatAppId")
                if (wechatAppId != null ){
                    WXAPiHandler.setupWxApi(wechatAppId,this)
                    WXAPiHandler.setCoolBool(true)
                    Log.d("fluwx","weChatAppId: $wechatAppId")
                }else {
                    Log.e("fluwx","can't load meta-data weChatAppId")
                }
            }
            WXAPiHandler.wxApi?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
            startSpecifiedActivity(defaultFlutterActivityAction())
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        try {
            WXAPiHandler.wxApi?.handleIntent(intent, this)
        } catch (e: Exception) {
            e.printStackTrace()
            startSpecifiedActivity(defaultFlutterActivityAction())
            finish()
        }
    }


    override fun onReq(baseReq: BaseReq) {
        // FIXME: 可能是官方的Bug，从微信拉起APP的Intent类型不对，无法跳转回Flutter Activity
        // 稳定复现场景：微信版本为7.0.5，小程序SDK为2.7.7
       FluwxRequestHandler.onReq(baseReq,this)
    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    override fun onResp(resp: BaseResp) {
        FluwxResponseHandler.handleResponse(resp)
        finish()
    }

    private fun startSpecifiedActivity(action: String, bundle: Bundle? = null, bundleKey: String? = null) {
        Intent(action).run {
            bundleKey?.let {
                putExtra(bundleKey, bundle)
            }
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            packageManager?.let {
                resolveActivity(packageManager)?.also {
                    startActivity(this)
                    finish()
                }
            }
        }
    }

    private fun defaultFlutterActivityAction(): String = "$packageName.FlutterActivity"
}
