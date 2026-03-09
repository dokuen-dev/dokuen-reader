# ProGuard Configuration for Plugins

When building release versions of your plugin with ProGuard or R8 enabled, you must keep certain
classes to ensure AIDL communication works correctly.

## Required ProGuard Rules

Add these rules to your plugin's `proguard-rules.pro` file:

```proguard
# Keep all AIDL-generated classes
-keep class * implements android.os.IInterface {
    *;
}

# Keep all AIDL parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep plugin API classes
-keep class io.github.dokuendev.dokuenreader.plugin.core.** { *; }
-keep class io.github.dokuendev.dokuenreader.ocr.** { *; }

# Keep your plugin service
-keep class your.package.name.YourOcrService {
    *;
}

# Keep AIDL stub classes
-keepclassmembers class * extends android.os.Binder {
    public <methods>;
}

# Keep callback interfaces
-keep interface io.github.dokuendev.dokuenreader.plugin.core.IInitCallback { *; }
-keep interface io.github.dokuendev.dokuenreader.ocr.IOcrCallback { *; }
-keep interface io.github.dokuendev.dokuenreader.ocr.IOcrService { *; }
```

## Example Configuration

For a plugin in package `com.example.myplugin`:

```proguard
# Keep all AIDL-generated classes
-keep class * implements android.os.IInterface {
    *;
}

# Keep all AIDL parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep plugin API classes (from dependencies)
-keep class io.github.dokuendev.dokuenreader.plugin.core.** { *; }
-keep class io.github.dokuendev.dokuenreader.ocr.** { *; }

# Keep your plugin service
-keep class com.example.myplugin.MyOcrService {
    *;
}

# Keep AIDL stub classes
-keepclassmembers class * extends android.os.Binder {
    public <methods>;
}

# Keep callback interfaces
-keep interface io.github.dokuendev.dokuenreader.plugin.core.IInitCallback { *; }
-keep interface io.github.dokuendev.dokuenreader.ocr.IOcrCallback { *; }
-keep interface io.github.dokuendev.dokuenreader.ocr.IOcrService { *; }

# If you use Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# If you use reflection for configuration
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
```

## Testing ProGuard Configuration

To verify your ProGuard rules are correct:

1. Build a release APK with ProGuard enabled
2. Install it on a test device
3. Try to bind to your plugin from the Dokuen app
4. Check logcat for any `ClassNotFoundException` or `NoSuchMethodException`

Common issues:

- **Service not binding**: Your service class was removed by ProGuard
- **Parcelable errors**: AIDL parcelables were obfuscated or removed
- **Method not found**: AIDL interface methods were removed

## Minimal Configuration

If you're unsure, this minimal configuration keeps everything:

```proguard
# Keep everything in plugin API packages
-keep class io.github.dokuendev.dokuenreader.plugin.core.** { *; }
-keep class io.github.dokuendev.dokuenreader.ocr.** { *; }

# Keep your entire plugin package (replace with your package)
-keep class com.example.myplugin.** { *; }
```

This is less optimal for APK size but guarantees compatibility.

## See Also

- [Android ProGuard Documentation](https://developer.android.com/studio/build/shrink-code)
- [AIDL and ProGuard](https://developer.android.com/guide/components/aidl#ProGuard)
