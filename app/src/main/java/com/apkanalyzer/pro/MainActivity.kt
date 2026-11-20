package com.apkanalyzer.pro

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 17, 33)
    private val panel = Color.rgb(20, 31, 53)
    private val blue = Color.rgb(55, 139, 255)
    private val white = Color.WHITE
    private lateinit var status: TextView
    private lateinit var details: TextView
    private lateinit var fileList: TextView
    private lateinit var extractButton: Button
    private var selectedUri: Uri? = null
    private var extractedDir: File? = null
    private var reportText: String = ""

    private val requestApk = 1001
    private val requestZip = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildUi()
    }


    @Deprecated("Using the platform document picker for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        when (requestCode) {
            requestApk -> {
                selectedUri = uri
                status.text = "تم اختيار ملف APK بنجاح"
                extractButton.isEnabled = true
                analyzeApk(uri)
            }
            requestZip -> exportZip(uri)
        }
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
        }
        scroll.addView(root)
        setContentView(scroll)

        val title = TextView(this).apply {
            text = "APK Analyzer Pro"
            textSize = 26f; setTextColor(white); gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        root.addView(title, matchWrap())
        root.addView(label("محلل ملفات تطبيقات أندرويد • يعمل دون إنترنت"), matchWrap())

        val choose = button("اختيار ملف APK")
        root.addView(choose, fullButton())
        choose.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                    "application/vnd.android.package-archive",
                    "application/octet-stream", "application/zip"
                ))
            }
            startActivityForResult(intent, requestApk)
        }

        status = label("لم يتم اختيار أي ملف بعد")
        root.addView(status, matchWrap())

        root.addView(section("معلومات التطبيق"))
        details = infoBox("اختر ملف APK لعرض معلوماته والصلاحيات والملفات الموجودة بداخله.")
        root.addView(details, matchWrap())

        extractButton = button("فك واستخراج جميع الملفات").apply { isEnabled = false }
        root.addView(extractButton, fullButton())
        extractButton.setOnClickListener { selectedUri?.let { extractApk(it) } }

        val export = button("تصدير الملفات في ZIP")
        root.addView(export, fullButton())
        export.setOnClickListener {
            if (extractedDir == null) {
                toast("فك ملف APK أولاً")
            } else {
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, "APKAnalyzerPro_Export.zip")
                }
                startActivityForResult(intent, requestZip)
            }
        }

        root.addView(section("محتويات ملف APK"))
        fileList = infoBox("ستظهر قائمة الملفات هنا بعد اختيار APK.")
        root.addView(fileList, matchWrap())
        root.addView(section("ملاحظات مهمة"))
        root.addView(infoBox(
            "• يستخرج التطبيق ملفات APK كما هي: الصور والموارد وملفات DEX وملفات XML وغيرها.\n" +
            "• يعرض معلومات الحزمة والصلاحيات التي يستطيع Android قراءتها.\n" +
            "• ملفات DEX تُستخرج بصيغتها الأصلية ولا يتم تحويلها تلقائياً إلى كود Kotlin/Java.\n" +
            "• بعض ملفات XML داخل APK تكون بصيغة Android الثنائية، وقد تحتاج أداة متخصصة مثل Apktool لفكها.\n" +
            "• استخدمه فقط مع التطبيقات التي تملكها أو لديك تصريح بتحليلها."
        ), matchWrap())
    }

    private fun analyzeApk(uri: Uri) {
        try {
            val pm = packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageArchiveInfo(uriToCache(uri).absolutePath,
                android.content.pm.PackageManager.GET_PERMISSIONS or
                    android.content.pm.PackageManager.GET_ACTIVITIES or
                    android.content.pm.PackageManager.GET_SERVICES or
                    android.content.pm.PackageManager.GET_RECEIVERS)
            val name = info?.applicationInfo?.loadLabel(pm)?.toString() ?: "غير متاح"
            val pkg = info?.packageName ?: "تعذر قراءة اسم الحزمة"
            @Suppress("DEPRECATION")
            val version = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info?.longVersionCode?.toString() ?: "غير معروف"
            } else {
                info?.versionCode?.toString() ?: "غير معروف"
            }
            val perms = info?.requestedPermissions?.joinToString("\n") { "• $it" }
                ?: "لم يتمكن Android من قراءة الصلاحيات"
            reportText = "اسم التطبيق: $name\nاسم الحزمة: $pkg\nرقم الإصدار: $version\n\nالصلاحيات:\n$perms"
            details.text = reportText
            val entries = readEntryNames(uri)
            fileList.text = if (entries.isEmpty()) "لم يتم العثور على ملفات." else
                entries.take(250).joinToString("\n") + if (entries.size > 250) "\n... والمزيد" else ""
        } catch (e: Exception) {
            details.text = "تعذر تحليل الملف: ${e.message ?: "خطأ غير معروف"}"
        }
    }

    private fun uriToCache(uri: Uri): File {
        val out = File(cacheDir, "selected.apk")
        contentResolver.openInputStream(uri).use { input ->
            if (input == null) throw IOException("لا يمكن فتح الملف")
            out.outputStream().use { output -> input.copyTo(output) }
        }
        return out
    }

    private fun readEntryNames(uri: Uri): List<String> {
        val names = mutableListOf<String>()
        contentResolver.openInputStream(uri).use { raw ->
            if (raw == null) return names
            ZipInputStream(BufferedInputStream(raw)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) names.add(entry.name)
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return names
    }

    private fun extractApk(uri: Uri) {
        try {
            val target = File(getExternalFilesDir(null), "APKAnalyzerPro/Extracted").apply {
                deleteRecursively(); mkdirs()
            }
            var count = 0
            var total = 0L
            contentResolver.openInputStream(uri).use { raw ->
                if (raw == null) throw IOException("تعذر قراءة الملف")
                ZipInputStream(BufferedInputStream(raw)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val safe = File(target, entry.name)
                        val canonicalTarget = target.canonicalPath + File.separator
                        if (!safe.canonicalPath.startsWith(canonicalTarget)) {
                            throw IOException("مسار غير آمن داخل APK")
                        }
                        if (entry.isDirectory) safe.mkdirs() else {
                            safe.parentFile?.mkdirs()
                            FileOutputStream(safe).use { output ->
                                val buffer = ByteArray(8192)
                                while (true) {
                                    val n = zip.read(buffer)
                                    if (n <= 0) break
                                    total += n
                                    if (total > 500L * 1024 * 1024) throw IOException("حجم الملفات المستخرجة تجاوز 500 ميجابايت")
                                    output.write(buffer, 0, n)
                                }
                            }
                            count++
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            extractedDir = target
            status.text = "اكتمل الاستخراج بنجاح"
            toast("تم استخراج $count ملف")
            fileList.text = target.walkTopDown().filter { it.isFile }.take(250)
                .joinToString("\n") { it.relativeTo(target).path }
        } catch (e: Exception) {
            toast("فشل الاستخراج: ${e.message}")
        }
    }

    private fun exportZip(destination: Uri) {
        try {
            val dir = extractedDir ?: return
            contentResolver.openOutputStream(destination)?.use { raw ->
                ZipOutputStream(BufferedOutputStream(raw)).use { zip ->
                    dir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val name = file.relativeTo(dir).invariantSeparatorsPath
                        zip.putNextEntry(ZipEntry(name))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            } ?: throw IOException("تعذر إنشاء ملف ZIP")
            toast("تم إنشاء ملف ZIP")
        } catch (e: Exception) {
            toast("فشل التصدير: ${e.message}")
        }
    }

    private fun label(textValue: String) = TextView(this).apply {
        text = textValue; textSize = 14f; setTextColor(Color.LTGRAY)
        gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(12))
    }
    private fun section(textValue: String) = TextView(this).apply {
        text = textValue; textSize = 18f; setTextColor(white)
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(8))
    }
    private fun infoBox(value: String) = TextView(this).apply {
        text = value; textSize = 13f; setTextColor(Color.rgb(220, 230, 245))
        setPadding(dp(14), dp(14), dp(14), dp(14)); setBackgroundColor(panel)
    }
    private fun button(value: String) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 15f
        setTextColor(white); setBackgroundTintList(android.content.res.ColorStateList.valueOf(blue))
    }
    private fun fullButton() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
    ).apply { topMargin = dp(8); bottomMargin = dp(5) }
    private fun matchWrap() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    )
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}