package com.ecjtu.sharebox.ui.adapter

import android.app.Activity
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.ecjtu.sharebox.R
import com.ecjtu.sharebox.network.AsyncNetwork
import com.ecjtu.sharebox.network.IRequestCallback
import com.ecjtu.sharebox.ui.dialog.ApDataDialog
import com.ecjtu.sharebox.ui.dialog.FilePickDialog
import com.ecjtu.sharebox.ui.dialog.InternetFilePickDialog
import com.ecjtu.sharebox.ui.dialog.TextItemDialog
import com.ecjtu.sharebox.util.cache.CacheUtil
import com.ecjtu.sharebox.util.file.FileUtil
import org.ecjtu.easyserver.server.ConversionFactory
import org.ecjtu.easyserver.server.DeviceInfo
import org.json.JSONObject
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.HttpURLConnection

/**
 * Created by Ethan_Xiang on 2017/7/3.
 */
class DeviceRecyclerViewAdapter : RecyclerView.Adapter<DeviceRecyclerViewAdapter.VH>, View.OnClickListener,
        View.OnLongClickListener {

    private var mDeviceList: MutableList<DeviceInfo>? = null

    private var mWeakRef: WeakReference<Activity>? = null

    constructor(list: MutableList<DeviceInfo>, activity: Activity) : super() {
        mDeviceList = list
        mWeakRef = WeakReference(activity)
    }

    override fun getItemCount(): Int {
        return mDeviceList?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): VH? {
        val v = LayoutInflater.from(parent?.context).inflate(R.layout.layout_device_item, parent, false)
        v.setOnClickListener(this)
        v.setOnLongClickListener(this)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH?, position: Int) {
        val info = mDeviceList?.get(position)

        val iconUrl = "${info?.ip}:${info?.port}${info?.icon}"
        holder?.itemView?.setTag(R.id.extra_tag, position)
        Glide.with(holder?.itemView?.context).load("http://" + iconUrl).
                apply(RequestOptions().placeholder(R.mipmap.logo).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE)).
                into(holder?.icon) // 规避缓存机制导致图片不刷新

        holder?.name?.setText(info?.name)

        if (info?.fileMap == null) {
            AsyncNetwork().requestDeviceInfo("${info?.ip}:${info?.port}", object : IRequestCallback {
                override fun onSuccess(httpURLConnection: HttpURLConnection?, response: String) {
                    ConversionFactory.json2DeviceInfo(JSONObject(response)).apply {
                        info?.fileMap = fileMap
                    }
//                    mWeakRef?.get()?.runOnUiThread {
//
//                    }
                }

                override fun onError(httpURLConnection: HttpURLConnection?, exception: Exception) {
                }
            })
        } else {
            //do nothing
        }
    }

    override fun onClick(v: View?) {
        val position = v?.getTag(R.id.extra_tag) as Int
        val deviceInfo = mDeviceList?.get(position)

        AsyncNetwork().requestDeviceInfo("${deviceInfo?.ip}:${deviceInfo?.port}", object : IRequestCallback {
            override fun onSuccess(httpURLConnection: HttpURLConnection?, response: String) {
                ConversionFactory.json2DeviceInfo(JSONObject(response)).apply {
                    deviceInfo?.fileMap = fileMap
                }
                mWeakRef?.get()?.runOnUiThread {
                    if (mWeakRef?.get() != null) {
                        InternetFilePickDialog(mWeakRef?.get()!!, mWeakRef?.get(), deviceInfo!!).apply {
                            val holders: MutableMap<String, FilePickDialog.TabItemHolder> = mutableMapOf()
                            if (deviceInfo.fileMap?.entries != null) {
                                for (entry in deviceInfo.fileMap!!.entries) {
                                    val type = FileUtil.string2MediaFileType(entry.key)
                                    val holder = FilePickDialog.TabItemHolder(entry.key, type,
                                            null,
                                            entry.value)
                                    holders.put(entry.key, holder)
                                }
                            }
                            setup(deviceInfo.name!!, holders)
                            show()
                        }
                    }
                }
            }

            override fun onError(httpURLConnection: HttpURLConnection?, exception: Exception) {
                mWeakRef?.get()?.runOnUiThread {
                    Toast.makeText(mWeakRef?.get()!!, R.string.client_has_not_yet_ready_try_later, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onLongClick(v: View?): Boolean {
        val position = v?.getTag(R.id.extra_tag) as Int
        val deviceInfo = mDeviceList?.get(position)
        TextItemDialog(v.context).apply {
            setupItem(arrayOf(v.context.getString(R.string.details), v.context.getString(R.string.cancel)))
            setOnClickListener { index ->
                if (index == 0) {
                    if (mWeakRef?.get() != null && mWeakRef!!.get() != null) {
                        ApDataDialog(v.context, mWeakRef?.get()!!).apply {
                            setup(deviceInfo!!.ip, deviceInfo.port)
                        }.show()
                    }
                }
                cancel()
            }
        }.show()

        return true
    }

    class VH(item: View) : RecyclerView.ViewHolder(item) {
        var icon: ImageView? = null
        var name: TextView? = null
        var thumb: ImageView? = null
        var fileCount: TextView? = null

        init {
            icon = item.findViewById(R.id.icon) as ImageView
            name = item.findViewById(R.id.name) as TextView
            thumb = item.findViewById(R.id.content) as ImageView
            fileCount = item.findViewById(R.id.file_count) as TextView
        }
    }
}


