package com.ecjtu.sharebox.ui.dialog

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.AsyncTask
import android.os.Message
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.TabLayout
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.ecjtu.sharebox.Constants
import com.ecjtu.sharebox.R
import com.ecjtu.sharebox.async.MemoryUnLeakHandler
import com.ecjtu.sharebox.getMainApplication
import com.ecjtu.sharebox.ui.adapter.FileExpandableAdapter
import com.ecjtu.sharebox.ui.view.FileExpandableListView
import com.ecjtu.sharebox.util.file.FileUtil
import org.ecjtu.easyserver.server.DeviceInfo
import org.ecjtu.easyserver.server.ServerManager
import java.io.File
import java.util.*
import kotlin.concurrent.thread


/**
 * Created by KerriGan on 2017/6/2.
 */
open class FilePickDialog : BaseBottomSheetDialog, Toolbar.OnMenuItemClickListener, MemoryUnLeakHandler.IHandleMessage {

    constructor(context: Context, activity: Activity? = null) : super(context, activity) {

    }

    private var mBehavior: BottomSheetBehavior<View>? = null

    private var mTabLayout: TabLayout? = null

    private var mViewPager: ViewPager? = null

    private var mTabItemHolders: MutableMap<String, TabItemHolder>? = mutableMapOf()

    private var mViewPagerViews = mutableMapOf<Int, View>()

    private var mBottomSheet: View? = null

    private var mExpandableListView: FileExpandableListView? = null

    private var mProgressBar: ProgressBar? = null

    private var mProgressDialog: ProgressDialog? = null

    private var mHasFindAll = false

    private var mRetMap: MutableMap<String, ArrayList<FileExpandableAdapter.VH>> = mutableMapOf()

    companion object {
        fun string2MediaFileType(str: String): FileUtil.MediaFileType? {
            var ret: FileUtil.MediaFileType? = null
            when (str) {
                "Movie" -> {
                    ret = FileUtil.MediaFileType.MOVIE
                }
                "Music" -> {
                    ret = FileUtil.MediaFileType.MP3
                }
                "Photo" -> {
                    ret = FileUtil.MediaFileType.IMG
                }
                "Doc" -> {
                    ret = FileUtil.MediaFileType.DOC
                }
                "Apk" -> {
                    ret = FileUtil.MediaFileType.APP
                }
                "Rar" -> {
                    ret = FileUtil.MediaFileType.RAR
                }
            }
            return ret
        }

        fun mediaFileType2String(type: FileUtil.MediaFileType): String? {
            var ret: String? = null
            when (type) {
                FileUtil.MediaFileType.MOVIE -> {
                    ret = "Movie"
                }
                FileUtil.MediaFileType.MP3 -> {
                    ret = "Music"
                }
                FileUtil.MediaFileType.IMG -> {
                    ret = "Photo"
                }
                FileUtil.MediaFileType.DOC -> {
                    ret = "Doc"
                }
                FileUtil.MediaFileType.APP -> {
                    ret = "Apk"
                }
                FileUtil.MediaFileType.RAR -> {
                    ret = "Rar"
                }
            }
            return ret
        }

    }

    override fun initializeDialog() {
        super.initializeDialog()
        context.setTheme(R.style.WhiteToolbar)
    }

    override fun onCreateView(): View? {
        windowTranslucent()

        var vg = layoutInflater.inflate(R.layout.dialog_file_pick, null)

        fullScreenLayout(vg)
        return vg
    }

    override fun onViewCreated(view: View?): Boolean {
        super.onViewCreated(view)
        mBehavior = BottomSheetBehavior.from(findViewById(android.support.design.R.id.design_bottom_sheet))
        mBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED

        val display = ownerActivity.getWindowManager().getDefaultDisplay()
        mBehavior?.peekHeight = display.height * 2 / 3

        mBottomSheet = findViewById(R.id.design_bottom_sheet)

        initView(view as ViewGroup)
        return true
    }

    open protected fun initData() {
        var item = TabItemHolder(context.getString(R.string.movie), string2MediaFileType("Movie"))
        mTabItemHolders?.put("Movie", item)

        item = TabItemHolder(context.getString(R.string.music), string2MediaFileType("Music"))
        mTabItemHolders?.put("Music", item)

        item = TabItemHolder(context.getString(R.string.photo), string2MediaFileType("Photo"))
        mTabItemHolders?.put("Photo", item)

        item = TabItemHolder(context.getString(R.string.doc), string2MediaFileType("Doc"))
        mTabItemHolders?.put("Doc", item)

        item = TabItemHolder(context.getString(R.string.apk), string2MediaFileType("Apk"))
        mTabItemHolders?.put("Apk", item)

        item = TabItemHolder(context.getString(R.string.rar), string2MediaFileType("Rar"))
        mTabItemHolders?.put("Rar", item)

    }

    open protected fun initView(vg: ViewGroup) {
        initData()

        var toolbar = vg.findViewById(R.id.toolbar) as Toolbar

        toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))

        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        toolbar.inflateMenu(R.menu.menu_file_pick)

        toolbar.setOnMenuItemClickListener(this)

        mBehavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            var mFitSystemWindow = false

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                if (slideOffset == 1.0f) {
                    mFitSystemWindow = true
                } else {
                    if (mFitSystemWindow != false) {
                        toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))
                        toolbar.fitsSystemWindows = false
                        toolbar.setPadding(toolbar.paddingLeft, 0, toolbar.paddingRight, toolbar.paddingBottom)
                    }
                    mFitSystemWindow = false
                }
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val attrsArray = intArrayOf(android.R.attr.homeAsUpIndicator)
                    val typedArray = context.obtainStyledAttributes(attrsArray)
                    val dw = typedArray.getDrawable(0)
                    toolbar.setNavigationIcon(dw)
                    toolbar.fitsSystemWindows = true
                    toolbar.requestFitSystemWindows()

                    // don't forget the resource recycling
                    typedArray.recycle()
                    return
                } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismiss()
                }

                if (!mFitSystemWindow) {
                    toolbar.setNavigationIcon(ColorDrawable(Color.TRANSPARENT))
                    toolbar.fitsSystemWindows = false
                    toolbar.setPadding(toolbar.paddingLeft, 0, toolbar.paddingRight, toolbar.paddingBottom)
                }
            }
        })

        mTabLayout = vg.findViewById(R.id.tab_layout) as TabLayout
        mViewPager = vg.findViewById(R.id.view_pager) as ViewPager
        mProgressBar = vg.findViewById(R.id.progress_bar) as ProgressBar

        mViewPager?.adapter = getViewPagerAdapter()

        mViewPager?.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                mExpandableListView = getListView(position)
                mExpandableListView?.loadedData()
            }
        })

        mTabLayout?.setupWithViewPager(mViewPager)
    }

    open fun getViewPagerAdapter(): PagerAdapter {
        return object : PagerAdapter() {

            override fun isViewFromObject(view: View?, `object`: Any?): Boolean {
                return view == `object`
            }

            override fun getCount(): Int {
                return mTabItemHolders?.size ?: 0
            }

            override fun getPageTitle(position: Int): CharSequence {
                var key = mTabItemHolders?.keys?.elementAt(position)!!
                return mTabItemHolders?.get(key)?.title as CharSequence
            }

            override fun instantiateItem(container: ViewGroup?, position: Int): Any {
                var vg: FileExpandableListView? = null

                vg = mViewPagerViews.get(position) as FileExpandableListView?
                if (vg == null) {
                    Log.e("ViewPager", "create view")
                    vg = layoutInflater.inflate(R.layout.layout_file_expandable_list_view, container, false) as FileExpandableListView
                }

                container?.addView(vg)
                mViewPagerViews.put(position, vg)
                if (mExpandableListView == null)
                    mExpandableListView = getListView(0) as FileExpandableListView

                var title = mTabItemHolders?.keys?.elementAt(position) as String

                var holder = mTabItemHolders?.get(title)
                vg.fileExpandableAdapter = getFileAdapter(vg, title)
                var oldCache :List<FileExpandableAdapter.VH>? =null
                if(isLoadCache()){
                    oldCache= getOldCacheAndClone(title)
                }else{
                    var fileList=mTabItemHolders?.get(title)?.fileList
                    if(fileList!=null){
                        var map=LinkedHashMap<String,MutableList<File>>()
                        oldCache= makeVhList(fileList,map)
                    }
                }

                vg.initData(holder,oldCache)

                if (mTabItemHolders?.get(title)?.task == null && mTabItemHolders?.get(title)?.fileList == null) {
                    var task = LoadingFilesTask(context, holder!!)
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    mTabItemHolders?.get(title)?.task = task
                }

                if (mHasFindAll) {
                    selectViewPager(vg)
                }

                refreshData()

                return vg
            }

            override fun destroyItem(container: ViewGroup?, position: Int, `object`: Any?) {
                var title = mTabItemHolders?.keys?.elementAt(position) as String
                if (mTabItemHolders?.get(title)?.task != null) {
                    var task = mTabItemHolders?.get(title)?.task
                    if (task?.status == AsyncTask.Status.FINISHED) {
                        //do nothing
                    } else {
                        task?.cancel(true)
                        mTabItemHolders?.get(title)?.task = null
                    }
                }
                container?.removeView(`object` as View)
            }
        }
    }

    inner class LoadingFilesTask : AsyncTask<List<File>?, Void, List<File>?> {
        private val TAG = "LoadingFilesTask"

        private var mType: FileUtil.MediaFileType? = null

        private var mContext: Context? = null

        private var mHolder: TabItemHolder? = null

        constructor(context: Context, holder: TabItemHolder) : super() {
            mType = holder.type
            mContext = context
            mHolder = holder
        }

        override fun doInBackground(vararg params: List<File>?): List<File>? {
            Log.e(TAG, mediaFileType2String(mType!!) + " task begin")
            publishProgress()
            var list: List<File>? = null

            findFilesWithType(mContext!!, mType!!, mTabItemHolders!!)

            if (!isCancelled)
                Log.e(TAG, mediaFileType2String(mType!!) + " task finished")
            return list
        }

        override fun onProgressUpdate(vararg values: Void?) {
            super.onProgressUpdate(*values)
            refresh(true)
        }

        override fun onPostExecute(result: List<File>?) {
            super.onPostExecute(result)
            refresh(false)
            var index = mViewPager?.currentItem
            getListView(index!!)?.loadedData()

            var count = 0
            for (entry in mTabItemHolders!!.entries) {
                var title = entry.key

                if (mTabItemHolders?.get(title)?.task != null && mTabItemHolders?.get(title)?.fileList != null) {
                    count++
                }
            }
            if (count == mTabItemHolders!!.entries.size) {
                findFinish()
            }
        }

        override fun onCancelled(result: List<File>?) {
            super.onCancelled(result)
            Log.e(TAG, mediaFileType2String(mType!!) + " task cancelled")
        }
    }

    data class TabItemHolder(var title: String? = null, var type: FileUtil.MediaFileType? = null
                             , var task: LoadingFilesTask? = null, var fileList: List<File>? = null)

    private fun findFilesWithType(context: Context, type: FileUtil.MediaFileType, map: MutableMap<String, TabItemHolder>) {
        var list: MutableList<File>? = null
        when (type) {
            FileUtil.MediaFileType.MOVIE -> {
                list = FileUtil.getAllMediaFile(context!!, null)
                map.get("Movie")?.fileList = list
            }
            FileUtil.MediaFileType.MP3 -> {
                list = FileUtil.getAllMusicFile(context!!, null)
                map.get("Music")?.fileList = list
            }
            FileUtil.MediaFileType.IMG -> {
//                    list=FileUtil.getAllImageFile(mContext!!,null)
                list = FileUtil.getImagesByDCIM(context!!)
                map.get("Photo")?.fileList = list
            }
            FileUtil.MediaFileType.DOC -> {
                list = FileUtil.getAllDocFile(context!!, null)
                map.get("Doc")?.fileList = list
            }
            FileUtil.MediaFileType.APP -> {
                list = FileUtil.getAllApkFile(context!!, null)
                map.get("Apk")?.fileList = list
            }
            FileUtil.MediaFileType.RAR -> {
                list = FileUtil.getAllRarFile(context!!, null)
                map.get("Rar")?.fileList = list
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        //resolve listView doesn't not support NestedScrolling
        var ret = false
        if (mExpandableListView?.getFirstVisiblePosition() == 0) {
            val topChildView = mExpandableListView?.getChildAt(0)
            ret = topChildView?.getTop() ?:0 >= 0
        }

        if (mBehavior?.state != BottomSheetBehavior.STATE_EXPANDED) {
            ret = true
        }

        if (ret) {
            return super.dispatchTouchEvent(ev)
        } else {
            return mBottomSheet?.dispatchTouchEvent(ev)!!
        }
    }

    override fun onStop() {
        super.onStop()
        var iter = mTabItemHolders?.iterator()
        while (iter?.hasNext() ?: false) {
            var obj = iter?.next()
            obj?.value?.task?.cancel(true)
        }
        mHandler?.removeCallbacksAndMessages(null)
        mHandler = null
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        var id = item?.itemId
        when (id) {
            R.id.ok -> {
                if (mTabItemHolders == null) return true
                var iter = mTabItemHolders?.iterator()
                while (iter?.hasNext() ?: false) {
                    var obj = iter?.next()
                    obj?.value?.task?.cancel(true)
                }
                var fileList = mutableListOf<File>()

                for (entry in mTabItemHolders!!.entries) {
                    var title = entry.key
                    var key = FileExpandableAdapter.EXTRA_VH_LIST + title
                    var vhList = mRetMap.get(key)
                    if (ownerActivity != null && vhList!=null) {
                        var application = ownerActivity.getMainApplication()
                        application.getSavedInstance().put(key, vhList!!)
                    }
                }

                for(entry in mViewPagerViews){
                    var pager=entry.value as FileExpandableListView
                    var adapter=pager.fileExpandableAdapter
                    var save=pager.fileExpandableAdapter.vhList
                    if (ownerActivity != null && save!=null) {
                        var application = ownerActivity.getMainApplication()
                        application.getSavedInstance().put(FileExpandableAdapter.EXTRA_VH_LIST + adapter.title, save)
                    }
                }

                var map = if (!mHasFindAll) updateFileMap(fileList, mTabItemHolders!!) else updateAllFileList(fileList, mTabItemHolders!!)
                var deviceInfo = ownerActivity.getMainApplication().getSavedInstance().
                        get(Constants.KEY_INFO_OBJECT) as DeviceInfo
                deviceInfo.fileMap = map

                ServerManager.getInstance().setSharedFileList(fileList)
                this@FilePickDialog.cancel()
                Toast.makeText(context, "选择成功", Toast.LENGTH_SHORT).show()
            }

            R.id.select_all -> {
                var msg = mHandler?.obtainMessage(MSG_FIND_ALL)
                mHandler?.sendMessageDelayed(msg, (Integer.MAX_VALUE).toLong())
                mProgressDialog = ProgressDialog(context, ownerActivity).apply {
                    setOnCancelListener {
                        var iter = mTabItemHolders?.iterator()
                        while (iter?.hasNext() ?: false) {
                            var obj = iter?.next()
                            obj?.value?.task?.cancel(true)
                        }
                        mHandler?.removeCallbacksAndMessages(null)
                    }
                    show()
                }

                var set = mTabItemHolders?.entries
                if (set != null) {
                    var index = 0
                    for (entry in set) {
                        var title = entry.key
                        if (entry.value.task == null && entry.value.fileList == null) {
                            var task = LoadingFilesTask(context, entry.value)
                            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                            mTabItemHolders?.get(title)?.task = task
                        } else {
                            index++
                        }
                    }
                    if (index == set.size) {
                        findFinish()
                    }
                }
            }
        }
        return true
    }

    private fun updateFileMap(fileList: MutableList<File>, itemHolder: MutableMap<String, FilePickDialog.TabItemHolder>): MutableMap<String, List<String>> {
        var map = mutableMapOf<String, List<String>>()
        var index = 0
        for (element in itemHolder!!.entries) {
            var strList = mutableListOf<String>()
            var pager: View? = mViewPagerViews.get(index++) ?: continue
            pager = pager as FileExpandableListView
            if (element.value.fileList == null) continue
            var fileArr = pager.fileExpandableAdapter.selectedFile
            for (file in fileArr) {
                if (fileList.indexOf(file) < 0)
                    fileList.add(file)

                strList.add(file.absolutePath)
            }
            map.put(element.key, strList)
        }
        var application = if (ownerActivity != null) ownerActivity.getMainApplication() else null
        for (element in itemHolder!!.entries) {
            var title = element.key
            var strList = mutableListOf<String>()

            if (application == null) continue

            var obj=application.getSavedInstance().get(FileExpandableAdapter.EXTRA_VH_LIST + title)
            var vhList = if(obj!=null) obj as List<FileExpandableAdapter.VH> else null

            if (vhList != null) {
                for (vh in vhList) {
                    var fList = vh.activatedList
                    for (file in fList) {
                        if (fileList.indexOf(file) < 0)
                            fileList.add(file)
                        strList.add(file.absolutePath)
                    }
                }
            }
            map.put(title, strList)
        }
        return map
    }

    private fun updateAllFileList(fileList: MutableList<File>, itemHolder: MutableMap<String, FilePickDialog.TabItemHolder>): MutableMap<String, List<String>> {
        var map = mutableMapOf<String, List<String>>()
        var application = if (ownerActivity != null) ownerActivity.getMainApplication() else null
        for (element in itemHolder!!.entries) {
            var title = element.key
            var strList = mutableListOf<String>()
            if (element.value.fileList == null) continue
            if (application == null) continue

            var vhList = application.getSavedInstance().get(FileExpandableAdapter.EXTRA_VH_LIST + title) as List<FileExpandableAdapter.VH>

            if (vhList != null) {
                for (vh in vhList) {
                    var fList = vh.activatedList
                    for (file in fList) {
                        if (fileList.indexOf(file) < 0)
                            fileList.add(file)
                        strList.add(file.absolutePath)
                    }
                }
            }
            map.put(title, strList)
        }
        return map
    }

    protected fun setTabItemsHolder(holder: MutableMap<String, TabItemHolder>) {
        mTabItemHolders = holder
    }

    fun refreshData() {
        var index = mViewPager?.currentItem as Int
        getListView(index)?.loadedData()
    }

    fun refresh(refresh: Boolean) {
        if (refresh) {
            mProgressBar?.visibility = View.VISIBLE
        } else {
            mProgressBar?.visibility = View.INVISIBLE
        }
    }

    open fun getListView(position: Int): FileExpandableListView? {
        mViewPagerViews.get(position)?.let {
            return mViewPagerViews.get(position) as FileExpandableListView
        }
        return null
    }

    open fun getFileAdapter(vg: FileExpandableListView, title: String): FileExpandableAdapter {
        return vg.fileExpandableAdapter.apply { setup(title) }
    }

    private var mHandler: MemoryUnLeakHandler<FilePickDialog>? = MemoryUnLeakHandler<FilePickDialog>(this)

    private val MSG_FIND_ALL = 0x10

    private val MSG_FIND_ALL_FINISH = 0x11

    override fun handleMessage(msg: Message) {
        when (msg.what) {
            MSG_FIND_ALL_FINISH -> {
                mProgressDialog?.cancel()
                for (entry in mViewPagerViews) {
                    selectViewPager(entry.value as FileExpandableListView)
                }
            }
            MSG_FIND_ALL -> {
            }
        }
    }

    fun findFinish() {
        if (mHandler != null && mHandler!!.hasMessages(MSG_FIND_ALL)) {
            mHasFindAll = true

            thread {
                mRetMap.clear()
                val res = LinkedHashMap<String, MutableList<File>>()
                for (entry in mTabItemHolders!!.entries) {
                    res.clear()
                    var title = entry.key
                    var fileList = mTabItemHolders?.get(title)?.fileList
                    if (fileList != null) {
                        var newArr = makeVhList(fileList, res)

                        if (newArr == null) {
                            continue
                        }
                        for (view in mViewPagerViews) {
                            var viewPager = view!!.value as FileExpandableListView
                            if (viewPager.fileExpandableAdapter.title.equals(title)) {
                                viewPager.fileExpandableAdapter.replaceVhList(newArr)
                            }
                        }
                        mRetMap.put(FileExpandableAdapter.EXTRA_VH_LIST + title, newArr as ArrayList<FileExpandableAdapter.VH>)
                    }
                }
                mHandler?.removeMessages(MSG_FIND_ALL)
                mHandler?.obtainMessage(MSG_FIND_ALL_FINISH)?.sendToTarget()
            }
        }
    }

    private fun selectViewPager(fileExpandableListView: FileExpandableListView) {
        fileExpandableListView.fileExpandableAdapter.selectAll(true)
    }

    private fun makeVhList(fileList:List<File>,map :LinkedHashMap<String, MutableList<File>>? =null) : List<FileExpandableAdapter.VH>?{
        var map2:LinkedHashMap<String, MutableList<File>>? =map
        if(map2==null) map2=LinkedHashMap<String, MutableList<File>>()
        val names = FileUtil.foldFiles(fileList as MutableList<File>, map2!!)
        names?.let {
            val newArr = ArrayList<FileExpandableAdapter.VH>()

            for (name in names!!.iterator()) {
                val vh = FileExpandableAdapter.VH(File(name), map2!!.get(name))
                vh.activate(true)
                newArr.add(vh)
            }
            return newArr
        }
        return null
    }

    open protected fun isLoadCache():Boolean{
        return true
    }

    private fun getOldCacheAndClone(title:String): List<FileExpandableAdapter.VH>?{
        var cache=ownerActivity.getMainApplication().getSavedInstance().get(FileExpandableAdapter.EXTRA_VH_LIST + title) as List<FileExpandableAdapter.VH>?
        var newList= arrayListOf<FileExpandableAdapter.VH>()
        if(cache!=null){
            for(vh in cache){
                var newVh=vh.clone() as FileExpandableAdapter.VH
                if(newVh!=null){
                    newList.add(newVh)
                }
            }
        }
        return newList
    }
}