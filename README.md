## ReadView

提供一个可扩展的小说阅读界面自定义View

### 功能

1. 支持阅读页面的页眉、页脚的自定义；
2. 支持多种翻页模式（目前只支持覆盖翻页，待支持）；

### 使用

activity_main.xml如下：

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".activity.MainActivity">

    <org.klee.readview.widget.ReadView
        android:id="@+id/read_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</FrameLayout>
```

新建一个item_view_page.xml，用于配置ReadPage（阅读页面）：

```xml
<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/white">

    <TextView
        android:id="@+id/page_header"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentTop="true"
        android:gravity="center"
        android:textSize="11sp"
        android:textColor="@color/black"
        android:text="页眉"
        android:lines="1"/>

    <org.klee.readview.widget.ContentView
        android:id="@+id/page_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingVertical="8dp"
        android:paddingHorizontal="10dp"
        android:layout_below="@id/page_header"
        android:layout_above="@id/page_footer"/>

    <RelativeLayout
        android:id="@+id/page_footer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_alignParentBottom="true"
        android:orientation="horizontal"
        android:paddingHorizontal="30dp">
        <TextClock
            android:id="@+id/page_footer_clock"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_alignParentStart="true"
            android:textColor="@color/black"
            android:textSize="11sp" />
        <TextView
            android:id="@+id/page_footer_process"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_alignParentEnd="true"
            android:textSize="11sp"
            android:textColor="@color/black"
            android:text="页脚"/>
    </RelativeLayout>

</RelativeLayout>
```

注意必须指定一个ContentView，该控件用于显示小说阅读界面的文字，而页眉、页脚视图是可选的，之后在onCreate()方法中调用ReadPage的initLayout()完成ReadView的初始化：

```kotlin
val readView = findViewById<ReadView>(R.id.read_view)
// 配置ReadPage
readView.initPage { readPage, _ ->
	readPage.initLayout(R.layout.item_view_page, R.id.page_content, 
                        R.id.page_header, R.id.page_footer)
}
```

initLayout()函数有四个参数，分别为整个layout文件的id、ContentView的id，页眉视图id、页脚视图id，其中页眉、页脚视图的id为可选参数。

初始化ReadPage以后，就可以调用openBook()方法打开小说了：

```kotlin
readView.openBook(SfacgLoader(591785))
```

该函数接受一个BookLoader类型的参数，BookLoader是一个接口，该接口定义如下：

```kotlin
interface BookLoader {
    // 加载小说目录信息
    fun loadBook(): BookData
    // 加载章节内容
    fun loadChapter(chapData: ChapData)
}
```

该函数还有两个可选参数，分别为章节序号、分页序号，指定这两个参数以后，就会直接显示指定内容了。

除此以外，还可以通过调用setProcess()函数进行章节跳转

```kotlin
readView.setProcess(10, 1)
```

