package com.gstory.file_preview

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tencent.smtt.export.external.TbsCoreSettings
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsDownloader
import com.tencent.smtt.sdk.TbsListener


class TbsManager private constructor() {

    var isInit: Boolean = false
    var mSharedPreferences: SharedPreferences? = null
    var mContext: Context? = null

    companion object {
        const val TBS_INSTALL_STATUS = "TBS_INSTALL_STATUS"
        private var INSTANCE: TbsManager? = null
        val instance: TbsManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            if (INSTANCE == null) {
                INSTANCE = TbsManager()
            }
            INSTANCE!!
        }
    }

    fun initTBS(app: Context, callBack: InitCallBack?) {
        mContext = app
        if (isInit) {
            callBack?.initFinish(true)
            return
        }
        if (mSharedPreferences == null) {
            mSharedPreferences = app.getSharedPreferences("myTBS", Context.MODE_PRIVATE)
        }
        val isInstall = mSharedPreferences?.getBoolean(TBS_INSTALL_STATUS, false)
        if (isInstall == true) {
            isInit = true
            callBack?.initFinish(true)
            return
        }
        QbSdk.reset(app)
        resetSdk()
        QbSdk.setTbsListener(object : TbsListener {
            override fun onDownloadFinish(i: Int) {
                //tbs内核下载完成回调
                Log.e("TBS内核", "下载完成$i")
            }

            override fun onInstallFinish(i: Int) {
                //内核安装完成回调，
                Log.e("TBS内核", "安装完成")
                isInit = true
                //内核安装完成回调，
                if (isInit) {
                    callBack?.initFinish(true)
                    val editor = mSharedPreferences?.edit()
                    editor?.putBoolean(TBS_INSTALL_STATUS, true)
                    editor?.apply()
                }
            }

            override fun onDownloadProgress(i: Int) {
                //下载进度监听
                Log.e("TBS内核", "下载进度 $i")
            }
        })
        val cb: QbSdk.PreInitCallback = object : QbSdk.PreInitCallback {
            /**
             * 预初始化结束
             * 由于X5内核体积较大，需要依赖网络动态下发，所以当内核不存在的时候，默认会回调false，此时将会使用系统内核代替
             * @param arg0 是否使用X5内核
             */
            override fun onViewInitFinished(arg0: Boolean) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.e("TBS内核", "加载内核是否成功:$arg0")
                isInit = arg0
                if (arg0) {
                    callBack?.initFinish(true)
                    val editor = mSharedPreferences?.edit()
                    editor?.putBoolean(TBS_INSTALL_STATUS, true)
                    editor?.apply()
                    Log.e("TBS内核", "initFinish:$arg0")
                } else {
                    if (TbsDownloader.needDownload(mContext, false) && !TbsDownloader.isDownloading()) {
                        initFinish()
                    }
                }
            }

            override fun onCoreInitFinished() {
                Log.e("TBS内核", "加载内核完成")
            }
        }
        //x5内核初始化接口
        QbSdk.initX5Environment(app, cb)

    }

    private fun resetSdk() {
        // 在调用TBS初始化、创建WebView之前进行如下配置
        val map: HashMap<String, Any> = HashMap()
        map[TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER] = true
        map[TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE] = true
        QbSdk.initTbsSettings(map)
        QbSdk.setDownloadWithoutWifi(true)
        QbSdk.disableAutoCreateX5Webview()
    }

    fun initFinish(): Boolean {
        val isInstall = mSharedPreferences?.getBoolean(TBS_INSTALL_STATUS, false)
        if (isInstall == true) {
            Log.i("TBS内核", "X5内核已经下载安装过了！")
            isInit = true
            return true
        }
        if (!isInit && !TbsDownloader.isDownloading()) {
            QbSdk.reset(mContext)
            resetSdk()
            TbsDownloader.startDownload(mContext)
        }
        return isInit
    }
}

interface InitCallBack {
    fun initFinish(b: Boolean)
}