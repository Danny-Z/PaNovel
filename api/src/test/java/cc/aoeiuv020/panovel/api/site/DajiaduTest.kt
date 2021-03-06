package cc.aoeiuv020.panovel.api.site

import org.junit.Test

/**
 * Created by AoEiuV020 on 2018.06.06-15:28:17.
 */
class DajiaduTest : BaseNovelContextText(Dajiadu::class) {
    @Test
    fun search() {
        search("都市")
        search("择天记", "猫腻", "20/20274")
        search("寒门崛起", "朱郎才尽", "23/23752")
    }

    @Test
    fun detail() {
        detail("20/20274", "20/20274", "择天记", "猫腻",
                "http://www.dajiadu.net/files/article/image/20/20274/20274s.jpg",
                "太始元年，有神石自太空飞来，分散落在人间，其中落在东土大陆的神石，上面镌刻着奇怪的图腾，人因观其图腾而悟道，后立国教。\n" +
                        "数千年后，十四岁的少年孤儿陈长生，为治病改命离开自己的师父，带着一纸婚约来到神都，从而开启了一个逆天强者的崛起征程。",
                "2017-05-07 00:00:00")
        detail("23/23752", "23/23752", "寒门崛起", "朱郎才尽",
                "http://www.dajiadu.net/files/article/image/23/23752/23752s.jpg",
                "这是一个就业路上屡被蹂躏的古汉语专业研究生，回到了明朝中叶，进入了山村一家幼童身体后的故事。\n" +
                        "木讷父亲泼辣娘，一水的极品亲戚，农家小院是非不少。好在，咱有几千年的历史积淀，四书五经八股文，专业也对口，幸又看得到气运。谁言寒门再难出贵子。\n" +
                        "国力上升垂拱而治；\n" +
                        "法纪松弛，官纪慵散；\n" +
                        "有几只奸臣，也闹点倭寇；\n" +
                        "但总体上可以说，这是士大夫自由滋生的沃土。\n" +
                        "一个寒门崛起的传奇也就从这里生长了。\n" +
                        "谨以此文向所有的穿越经典致敬。",
                "2018-06-06 00:00:00")
    }

    @Test
    fun chapters() {
        chapters("20/20274", "序 下山", "20/20274/5294150", null,
                "后记", "20/20274/9642062", null,
                1190)
        chapters("23/23752", "世界有你们，真是美好", "23/23752/6643184", null,
                "第九百零八章 神转折（五）", "23/23752/11507981", null,
                932)
    }

    @Test
    fun content() {
        content("20/20274/5294150",
                "世界是相对的。",
                "十四岁的少年道士，下山。q",
                152)
        content("23/23752/11507981",
                "矫若惊龙，漂若浮云，宣纸上的“咏箸”宛若一条蛟龙破纸而出，搅动一室浩然正气和灵气，震惊了场中众人。",
                "bq",
                62)
    }

}