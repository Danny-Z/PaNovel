package cc.aoeiuv020.panovel.list

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.bookshelf.RefreshingDotView
import cc.aoeiuv020.panovel.data.entity.Novel
import cc.aoeiuv020.panovel.settings.ItemAction
import cc.aoeiuv020.panovel.settings.ListSettings
import cc.aoeiuv020.panovel.text.CheckableImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.novel_item_big.view.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.dip
import java.util.*
import java.util.concurrent.TimeUnit


class NovelViewHolder(itemView: View,
                      dotColor: Int,
                      dotSize: Float,
                      initItem: (NovelViewHolder) -> Unit = {},
                      actionDoneListener: (ItemAction, NovelViewHolder) -> Unit = { _, _ -> },
                      onError: (String, Throwable) -> Unit
) : RecyclerView.ViewHolder(itemView), AnkoLogger {
    private val itemListener = DefaultNovelItemActionListener(actionDoneListener, onError)

    // 所有View可空，准备支持不同布局，小的布局可能大部分View都没有，
    val name: TextView? = itemView.tvName
    val author: TextView? = itemView.tvAuthor
    val site: TextView? = itemView.tvSite
    val image: ImageView? = itemView.ivImage
    val last: TextView? = itemView.tvLast
    val checkUpdate: TextView? = itemView.tvCheckUpdate
    val readAt: TextView? = itemView.tvReadAt
    val star: CheckableImageView? = itemView.ivStar
    val refreshingDot: RefreshingDotView? = itemView.rdRefreshing
    // 包括刷新小红点和加入书架的爱心的FrameLayout,
    val flDot: View? = itemView.flDot
    // 提供外面的加调方法使用，
    lateinit var novel: Novel
        private set
    val ctx: Context = itemView.context

    init {
        // 这里的引用的设置修改后不会马上生效，因为ViewHolder会被复用，
        // 无所谓了，要是从外面传进来的话就太烦了，

        val typedValue = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        // 长按时波纹背景，
        val selectableItemBackground = typedValue.resourceId

        if (ListSettings.onDotClick != ItemAction.None) {
            refreshingDot?.setOnClickListener {
                itemListener.onDotClick(this)
            }
            refreshingDot?.setBackgroundResource(selectableItemBackground)
        }

        if (ListSettings.onDotLongClick != ItemAction.None) {
            refreshingDot?.setOnLongClickListener {
                itemListener.onDotLongClick(this)
            }
            refreshingDot?.setBackgroundResource(selectableItemBackground)
        }

        if (ListSettings.onCheckUpdateClick != ItemAction.None) {
            checkUpdate?.setOnClickListener {
                itemListener.onCheckUpdateClick(this)
            }
            checkUpdate?.setBackgroundResource(selectableItemBackground)
        }

        if (ListSettings.onNameClick != ItemAction.None) {
            name?.setOnClickListener {
                itemListener.onNameClick(this)
            }
            // 格子视图小说名的背景是渐变黑，不能改成波纹，
        }

        if (ListSettings.onNameLongClick != ItemAction.None) {
            name?.setOnLongClickListener {
                itemListener.onNameLongClick(this)
            }
            // 格子视图小说名的背景是渐变黑，不能改成波纹，
        }

        if (ListSettings.onLastChapterClick != ItemAction.None) {
            last?.setOnClickListener {
                itemListener.onLastChapterClick(this)
            }
            last?.setBackgroundResource(selectableItemBackground)
        }

        if (ListSettings.onItemClick != ItemAction.None) {
            itemView.setOnClickListener {
                itemListener.onItemClick(this)
            }
            itemView.setBackgroundResource(selectableItemBackground)
        }

        if (ListSettings.onItemLongClick != ItemAction.None) {
            itemView.setOnLongClickListener {
                itemListener.onItemLongClick(this)
            }
            itemView.setBackgroundResource(selectableItemBackground)
        }

        // TODO: star控件改成支持onCheckChanged，这样的话，要试试外部调用移出书架指定isChecked会不会调用click事件，
        star?.setOnClickListener {
            it as CheckableImageView
            it.toggle()
            itemListener.onStarChanged(this, it.isChecked)
        }
        refreshingDot?.setDotColor(dotColor)
        refreshingDot?.setDotSize(ctx.dip(dotSize))

        initItem(this)
    }

    fun apply(novel: Novel, refreshTime: Date) {
        debug { "apply <${novel.run { "$site.$author.$name.$checkUpdateTime" }}>, refreshTime = $refreshTime" }
        apply(novel)

        // 用tag防止复用vh导致异步冲突，
        // 如果没开始刷新或者刷新结束，tag会是null,
        // 如果刷新结束前vh被复用，tag就是非空且不等于当前novel,
        // 如果刷新结束前vh被复用，且刷新了新小说且刷新没结束就被原来的小说复用，tag等于novel，不重复请求刷新，
        when {
            itemView.tag === novel -> refreshing()
        // 手动刷新后需要联网更新，
            refreshTime > novel.checkUpdateTime -> refresh()
        // 询问服务器是否有更新，
            else -> askUpdate()
        }
    }

    private fun apply(novel: Novel) {
        this.novel = novel
        name?.text = novel.name
        author?.text = novel.author
        site?.text = novel.site
        last?.text = novel.lastChapterName
        image?.let { imageView ->
            Glide.with(ctx.applicationContext)
                    .load(novel.image)
                    .apply(RequestOptions().apply {
                        placeholder(R.drawable.ic_read)
                    })
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                            // TODO: 考虑在特定某些异常出现时直接改数据库里的小说图片地址，
                            novel.image = "https://www.snwx8.com/modules/article/images/nocover.jpg"
                            Glide.with(ctx.applicationContext).load(novel.image)
                                    .into(target)
                            return true
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                    })
                    .into(imageView)
        }
        star?.isChecked = novel.bookshelf
        // 显示“x分钟前”，
        checkUpdate?.text = DateUtils.getRelativeTimeSpanString(novel.checkUpdateTime.time, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(1))
        readAt?.text = novel.readAtChapterName
    }

    private fun refreshing() {
        debug { "refreshing ${name?.text}" }
        // 显示正在刷新，
        refreshingDot?.refreshing()
    }

    /**
     * 主动刷新，
     * 可以在itemListener里调用以刷新这本小说，
     */
    fun refresh() {
        debug { "refresh ${name?.text}" }
        refreshing()
        // 首次新结束时tag为null, 不能直接返回，
        // 被复用时tag可能非空且不等于novel,
        if (itemView.tag != null && itemView.tag !== novel) {
            return
        }
        itemView.tag = novel
        itemListener.requireRefresh(this)
    }

    /**
     * 询问服务器是否有更新，
     */
    private fun askUpdate() {
        debug { "askUpdate ${name?.text}" }
        refreshing()
        itemView.tag = novel
        itemListener.askUpdate(this)
    }

    /**
     * 刷新结束时调用，
     */
    fun refreshed(novel: Novel) {
        debug { "refreshed ${novel.name}" }
        // 首次新结束时tag为null, 不能直接返回，
        // 被复用时tag可能非空且不等于novel,
        if (itemView.tag != null && itemView.tag !== novel) {
            return
        }
        itemView.tag = null
        // 刷新小说相关信息，
        apply(novel)
        // 显示是否有更新，
        refreshingDot?.refreshed(this.novel.receiveUpdateTime > this.novel.readTime)
        // TODO: 根据是否刷出章节，移动item,
    }

    /**
     * 外部调用，小说移出书架，
     */
    fun removeBookshelf() {
        star?.isChecked = false
        itemListener.onStarChanged(this, false)
    }

    fun addBookshelf() {
        star?.isChecked = true
        itemListener.onStarChanged(this, true)
    }
}