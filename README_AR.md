# APK Analyzer Pro

مشروع Android كامل المصدر لتحليل واستخراج محتويات ملفات APK محلياً.

## المتطلبات
- Android Studio حديث
- JDK 17
- Android SDK Platform 35
- Gradle Wrapper يمكن إنشاؤه من Android Studio أو بالأمر `gradle wrapper --gradle-version 8.14`

## البناء
افتح المجلد `APKAnalyzerPro` في Android Studio ثم انتظر مزامنة Gradle، وبعدها:
`Build > Build Bundle(s) / APK(s) > Build APK(s)`

## الوظائف
- اختيار ملف APK عبر منتقي ملفات Android.
- قراءة اسم الحزمة والإصدار والصلاحيات حيثما أمكن.
- عرض أسماء العناصر داخل حاوية APK.
- استخراج محتويات APK إلى مساحة التطبيق الخاصة.
- تصدير الملفات المستخرجة إلى ZIP عبر منتقي الملفات.

## حدود مهمة
هذا الإصدار يستخرج ملفات APK الأصلية، ولا يدّعي استعادة المشروع الأصلي أو تحويل DEX إلى Kotlin/Java. ملفات Android XML الثنائية لا تتحول إلى XML مقروء في هذا الإصدار. لاستخدام فك موارد/Smali متقدم، يلزم دمج Apktool وBaksmali واختبار توافقهما وإصداراتهما بشكل منفصل.
