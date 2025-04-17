package com.flowers.splashing.tears.phoneinfor

import android.os.Build
import java.io.File
import android.opengl.GLES10
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
object CpuUtils {
    data class CpuStat(
        val total: Long,    // 总 CPU 时间
        val idle: Long      // 空闲时间
    )

    fun readCpuStat(): CpuStat {
        val statLine = File("/proc/stat").readLines().first()
        val parts = statLine.split("\\s+".toRegex())

        // 计算总 CPU 时间（user + nice + system + idle + iowait + irq + softirq + steal）
        val total = parts.subList(1, 8).sumOf { it.toLongOrNull() ?: 0L }
        // 空闲时间 = idle + iowait
        val idle = parts[4].toLongOrNull() ?: 0L + parts[5].toLongOrNull()!! ?: 0L

        return CpuStat(total, idle)
    }
    fun calculateCpuUsage(prevStat: CpuStat, currentStat: CpuStat): Int {
        val deltaTotal = currentStat.total - prevStat.total
        val deltaIdle = currentStat.idle - prevStat.idle
        return if (deltaTotal == 0L) 0 else ((deltaTotal - deltaIdle) * 100 / deltaTotal).toInt()
    }
    fun getFullGpuInfo(): Map<String, String> {
        return getGpuInfo()  // 优先使用 OpenGL 方法
            .takeIf { it["Vendor"] != "Unknown" }
            ?: getGpuInfoViaSystemProperties()
                .takeIf { it["Vendor"] != "Unknown" }
            ?: getGpuInfoFromProc()
                .takeIf { it["Vendor"] != "Unknown" }
            ?: getGpuInfoFromBuild()
    }
    fun getGpuInfoFromBuild(): Map<String, String> {
        val vendor = Build.MANUFACTURER  // 如 "Qualcomm"
        val model = Build.HARDWARE       // 如 "qcom"（非 GPU 专用字段）

        return mapOf(
            "Vendor" to vendor,
            "Renderer" to model,
            "Version" to "Unknown"
        )
    }
    fun getGpuInfoFromProc(): Map<String, String> {
        return try {
            val gpuInfo = File("/proc/gpuinfo").readText()
            val vendor = Regex("Vendor: (.+)").find(gpuInfo)?.groupValues?.get(1) ?: "Unknown"
            val renderer = Regex("Renderer: (.+)").find(gpuInfo)?.groupValues?.get(1) ?: "Unknown"
            val version = Regex("Version: (.+)").find(gpuInfo)?.groupValues?.get(1) ?: "Unknown"

            mapOf(
                "Vendor" to vendor,
                "Renderer" to renderer,
                "Version" to version
            )
        } catch (e: Exception) {
            mapOf(
                "Vendor" to "Unknown",
                "Renderer" to "Unknown",
                "Version" to "Unknown"
            )
        }
    }
    fun getGpuInfoViaSystemProperties(): Map<String, String> {
        return try {
            val systemClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemClass.getMethod("get", String::class.java)

            val vendor = getMethod.invoke(null, "ro.hardware.gpu") as? String ?: "Unknown"
            val model = getMethod.invoke(null, "ro.hardware.gpu.model") as? String ?: "Unknown"
            val driver = getMethod.invoke(null, "ro.gfx.driver.version") as? String ?: "Unknown"

            mapOf(
                "Vendor" to vendor,
                "Renderer" to model,
                "Version" to driver
            )
        } catch (e: Exception) {
            mapOf(
                "Vendor" to "Unknown",
                "Renderer" to "Unknown",
                "Version" to "Unknown"
            )
        }
    }


    fun getGpuInfo(): Map<String, String> {
        val egl = EGLContext.getEGL() as EGL10
        val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        egl.eglInitialize(display, null)

        // 获取 GPU Vendor 和 Renderer
        val vendor = GLES10.glGetString(GLES10.GL_VENDOR) ?: "Unknown"
        val renderer = GLES10.glGetString(GLES10.GL_RENDERER) ?: "Unknown"
        val version = GLES10.glGetString(GLES10.GL_VERSION) ?: "Unknown"

        egl.eglTerminate(display)

        return mapOf(
            "Vendor" to vendor,    // 如 "Qualcomm", "ARM", "NVIDIA"
            "Renderer" to renderer, // 如 "Adreno (TM) 650", "Mali-G78"
            "Version" to version    // 如 "OpenGL ES 3.2"
        )
    }


    fun getFullCpuModel(): String {
        return getCpuModelName()  // 优先读取 /proc/cpuinfo
            .takeIf { it != "Unknown" }
            ?: getCpuModelFromBuild()
                .takeIf { it != "Unknown" }
            ?: getCpuModelViaShell()
    }
    fun getCpuModelViaShell(): String {
        return try {
            val process = Runtime.getRuntime().exec("cat /proc/cpuinfo")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.useLines { lines ->
                lines.firstOrNull { it.contains("Hardware") }?.substringAfter(":")?.trim() ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }
    }
    fun getCpuModelFromBuild(): String {
        return when {
            Build.HARDWARE.contains("qcom") -> "Qualcomm ${Build.HARDWARE}"
            Build.HARDWARE.contains("exynos") -> "Samsung ${Build.HARDWARE}"
            Build.HARDWARE.contains("mt") -> "MediaTek ${Build.HARDWARE}"
            else -> "Unknown"
        }
    }
    fun getQualcommCpuName(defaultModel: String): String {
        return try {
            // 反射读取系统属性
            val systemClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemClass.getMethod("get", String::class.java)
            val platform = getMethod.invoke(null, "ro.board.platform") as? String ?: ""

            // 映射高通平台代号（如 "kona" -> Snapdragon 888）
            when (platform) {
                "kona" -> "Qualcomm Snapdragon 888"
                "lahaina" -> "Qualcomm Snapdragon 8 Gen 1"
                "taro" -> "Qualcomm Snapdragon 8+ Gen 1"
                else -> defaultModel
            }
        } catch (e: Exception) {
            defaultModel
        }
    }
    fun getCpuModelName(): String {
        return try {
            // 读取 /proc/cpuinfo
            val cpuInfo = File("/proc/cpuinfo").readText()

            // 匹配 "Hardware" 或 "model name" 字段
            val model = Regex("(Hardware|Processor|model name)\\s*:\\s*(.+)")
                .find(cpuInfo)
                ?.groupValues
                ?.get(2)  // 提取冒号后的值
                ?.trim()
                ?: "Unknown"

            // 处理特殊情况（如高通芯片）
            if (model.contains("Qualcomm")) {
                getQualcommCpuName(model)  // 解析高通芯片型号（见方法 2）
            } else {
                model
            }
        } catch (e: Exception) {
            "Unknown (${e.message})"
        }
    }
}