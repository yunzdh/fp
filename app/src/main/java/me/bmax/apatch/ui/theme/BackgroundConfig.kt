package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 背景配置管理类
 */
object BackgroundConfig {
    var customBackgroundUri: String? by mutableStateOf(null)
        private set
    var isCustomBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var customBackgroundOpacity: Float by mutableStateOf(0.5f)
        private set
    var customBackgroundDim: Float by mutableStateOf(0.2f)
        private set
    
    private const val PREFS_NAME = "background_settings"
    private const val KEY_CUSTOM_BACKGROUND_URI = "custom_background_uri"
    private const val KEY_CUSTOM_BACKGROUND_ENABLED = "custom_background_enabled"
    private const val KEY_CUSTOM_BACKGROUND_OPACITY = "custom_background_opacity"
    private const val KEY_CUSTOM_BACKGROUND_DIM = "custom_background_dim"
    private const val TAG = "BackgroundConfig"
    
    /**
     * 更新自定义背景URI
     */
    fun updateCustomBackgroundUri(uri: String?) {
        customBackgroundUri = uri
        isCustomBackgroundEnabled = uri != null
    }
    
    /**
     * 启用/禁用自定义背景
     */
    fun setCustomBackgroundEnabledState(enabled: Boolean) {
        isCustomBackgroundEnabled = enabled
    }

    /**
     * 设置自定义背景不透明度
     */
    fun setCustomBackgroundOpacityValue(opacity: Float) {
        customBackgroundOpacity = opacity
    }

    /**
     * 设置自定义背景暗度
     */
    fun setCustomBackgroundDimValue(dim: Float) {
        customBackgroundDim = dim
    }
    
    /**
     * 保存配置到SharedPreferences
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_CUSTOM_BACKGROUND_URI, customBackgroundUri)
            putBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, isCustomBackgroundEnabled)
            putFloat(KEY_CUSTOM_BACKGROUND_OPACITY, customBackgroundOpacity)
            putFloat(KEY_CUSTOM_BACKGROUND_DIM, customBackgroundDim)
            apply()
        }
    }
    
    /**
     * 从SharedPreferences加载配置
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_CUSTOM_BACKGROUND_URI, null)
        val enabled = prefs.getBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, false)
        val opacity = prefs.getFloat(KEY_CUSTOM_BACKGROUND_OPACITY, 0.5f)
        val dim = prefs.getFloat(KEY_CUSTOM_BACKGROUND_DIM, 0.2f)
        
        Log.d(TAG, "加载背景配置: URI=$uri, enabled=$enabled, opacity=$opacity, dim=$dim")
        
        customBackgroundUri = uri
        isCustomBackgroundEnabled = enabled
        customBackgroundOpacity = opacity
        customBackgroundDim = dim
    }
    
    /**
     * 重置配置
     */
    fun reset() {
        customBackgroundUri = null
        isCustomBackgroundEnabled = false
        customBackgroundOpacity = 0.5f
        customBackgroundDim = 0.2f
    }
}

/**
 * 背景管理器
 */
object BackgroundManager {
    private const val TAG = "BackgroundManager"
    private const val BACKGROUND_FILENAME = "background.jpg"

    /**
     * 获取背景文件
     */
    private fun getBackgroundFile(context: Context): File {
        return File(context.filesDir, BACKGROUND_FILENAME)
    }
    
    /**
     * 保存并应用自定义背景
     */
    suspend fun saveAndApplyCustomBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val savedUri = saveImageToInternalStorage(context, uri)
                if (savedUri != null) {
                    Log.d(TAG, "图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateCustomBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    Log.d(TAG, "背景配置保存成功，URI: ${BackgroundConfig.customBackgroundUri}, 启用状态: ${BackgroundConfig.isCustomBackgroundEnabled}")
                    true
                } else {
                    Log.e(TAG, "图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存自定义背景失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 清除自定义背景
     */
    fun clearCustomBackground(context: Context) {
        try {
            // 删除背景文件
            val file = getBackgroundFile(context)
            if (file.exists()) {
                file.delete()
            }
            
            // 重置配置
            BackgroundConfig.reset()
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除自定义背景失败: ${e.message}", e)
        }
    }
    
    /**
     * 加载自定义背景
     */
    fun loadCustomBackground(context: Context) {
        BackgroundConfig.load(context)
    }
    
    /**
     * 保存图片到内部存储
     */
    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val file = getBackgroundFile(context)

                FileOutputStream(file).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                inputStream.close()

                // 返回带时间戳的URI，确保Compose重组
                val fileUri = Uri.fromFile(file).buildUpon()
                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                    .build()
                    
                Log.d(TAG, "图片保存成功，文件URI: $fileUri")
                fileUri
            } catch (e: Exception) {
                Log.e(TAG, "保存图片到内部存储失败: ${e.message}", e)
                null
            }
        }
    }
}