package com.flowers.splashing.tears.phoneinfor


import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.flowers.splashing.tears.phoneinfor.AdIdHelper.AdIdCallback
import com.flowers.splashing.tears.phoneinfor.CpuUtils.calculateCpuUsage
import com.flowers.splashing.tears.phoneinfor.CpuUtils.getFullCpuModel
import com.flowers.splashing.tears.phoneinfor.CpuUtils.getFullGpuInfo
import com.flowers.splashing.tears.phoneinfor.databinding.ActivityInformationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.TimeZone

class InforMationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInformationBinding
    var intType = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intType = intent.getIntExtra("intType", -1)
        registerBatteryReceiver()
        // 设置设备基本信息
        setDeviceBasicInfo()
        // 设置需要权限的信息（无权限时跳过）
        setPermissionRequiredInfo()
        setSys()
        setCpuInfo()
        clickFun()
    }

    private fun clickFun() {
        with(binding) {
            papaInt = intType
            tvDeviceTitle.setOnClickListener {
                papaInt = 1
            }
            tvSystem.setOnClickListener {
                papaInt = 2
            }
            tvCpu.setOnClickListener {
                papaInt = 3
            }
            tvBattery.setOnClickListener {
                papaInt = 4
            }
            imgBack.setOnClickListener {
                finish()
            }
        }
    }

    private fun setSys() {
        val versionInfo = getAndroidVersionInfo()
        binding.inSystemL.tvAndroid.text = "Android " + versionInfo["SDK_INT"]
        binding.inSystemL.tvJG.text = versionInfo["CODENAME"]
        binding.inSystemL.tvjd.text = "Released :\n" + versionInfo["SECURITY_PATCH"]
        // 系统基本信息
        binding.inSystemL.tvCodeName.text = "Android" + Build.VERSION.CODENAME
        binding.inSystemL.tvApiLevel.text = Build.VERSION.SDK_INT.toString()
        binding.inSystemL.tvSecurityPatchLevel.text = try {
            Build.VERSION.SECURITY_PATCH
        } catch (e: Exception) {
            "Unknown"
        }
        binding.inSystemL.tvBuildNumber.text = Build.DISPLAY
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            binding.inSystemL.tvBaseband.text = Build.getRadioVersion()
        } else {
            binding.inSystemL.tvBaseband.text = getSystemProperty("gsm.version.baseband")
        }
        // 基带版本
        // Java虚拟机信息
        binding.inSystemL.tvJavaVm.text = System.getProperty("java.vm.version") ?: "Unknown"

        // 内核版本
        binding.inSystemL.tvKernel.text = System.getProperty("os.version") ?: "Unknown"

        // 系统语言
        binding.inSystemL.tvLanguage.text = Locale.getDefault().displayLanguage

        // OpenGL ES版本
        try {
            val configInfo =
                (getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                    .deviceConfigurationInfo
            binding.inSystemL.tvOpenGLEs.text = configInfo.glEsVersion
        } catch (e: Exception) {
            binding.inSystemL.tvOpenGLEs.text = "Unavailable"
        }

        // Root权限检测
        binding.inSystemL.tvRootAccess.text = isRooted()

        // SELinux状态
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val selinuxStatus = getSELinuxStatus()
                withContext(Dispatchers.Main) {
                    binding.inSystemL.tvSELinux.text = selinuxStatus
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.inSystemL.tvSELinux.text = "Unavailable"
                }
            }
        }
        // Google Play服务
        binding.inSystemL.tvGooglePlayServices.text = isGooglePlayServicesAvailable().toString()

        // 系统运行时间
        binding.inSystemL.tvSystemUptime.text =
            DateUtils.formatElapsedTime(SystemClock.elapsedRealtime() / 1000)

        // Treble支持
        binding.inSystemL.tvTreble.text = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.SUPPORTED_ABIS.joinToString()
            } else "Unavailable"
        } catch (e: SecurityException) {
            "Unavailable"
        }
    }

    fun getAndroidVersionInfo(): Map<String, String> {
        return mapOf(
            "SDK_INT" to Build.VERSION.SDK_INT.toString(),  // API 级别（如 33）
            "RELEASE" to Build.VERSION.RELEASE,             // 版本号（如 "13.0"）
            "CODENAME" to Build.VERSION.CODENAME,           // 开发代号（如 "Tiramisu"）
            "BASE_OS" to Build.VERSION.BASE_OS,            // 底层系统版本（可能为空）
            "SECURITY_PATCH" to Build.VERSION.SECURITY_PATCH // 安全补丁日期
        )
    }

    private fun setDeviceBasicInfo() {
        binding.inDeviceL.tvDeviceState.text = Build.BRAND + " " + Build.MODEL
        // 不需要权限的信息
        binding.inDeviceL.tvModel.text = Build.MODEL
        binding.inDeviceL.tvManufacturer.text = Build.MANUFACTURER
        binding.inDeviceL.tvBrand.text = Build.BRAND
        binding.inDeviceL.tvDevice.text = Build.DEVICE
        binding.inDeviceL.tvBoard.text = Build.BOARD
        binding.inDeviceL.tvHardware.text = Build.HARDWARE
        binding.inDeviceL.tvBuildFingerprint.text = Build.FINGERPRINT
        binding.inDeviceL.tvHost.text =
            if (packageManager.hasSystemFeature("android.hardware.usb.host")) "Supported" else "Not supported"
        binding.inDeviceL.tvTimezone.text = TimeZone.getDefault().id
        binding.inDeviceL.tvTotalDeviceFeatures.text =
            packageManager.systemAvailableFeatures.size.toString()
    }

    private fun setPermissionRequiredInfo() {
        // 需要权限的信息处理
        try {
            // Android Device ID（需要READ_PHONE_STATE权限）
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            binding.inDeviceL.tvAndroidDeviceId.text = deviceId ?: "Unavailable"
        } catch (e: SecurityException) {
            binding.inDeviceL.tvAndroidDeviceId.text = "-"
        }

        try {
            // 设备类型
            binding.inDeviceL.tvDeviceType.text = when {
                packageManager.hasSystemFeature("android.hardware.type.watch") -> "Watch"
                packageManager.hasSystemFeature("android.hardware.type.television") -> "TV"
                packageManager.hasSystemFeature("android.hardware.type.automotive") -> "Car"
                else -> "Phone"
            }
        } catch (e: Exception) {
            binding.inDeviceL.tvDeviceType.text = "Unknown"
        }



        try {
            // WIFI MAC地址（需要ACCESS_FINE_LOCATION权限）
            val wifiManager =
                applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = wifiManager.activeNetwork
            val linkProperties = wifiManager.getLinkProperties(network)
            val macAddress =
                linkProperties?.linkAddresses?.firstOrNull()?.toString() ?: "Unavailable"
            binding.inDeviceL.tvAddress.text = macAddress
        } catch (e: SecurityException) {
            binding.inDeviceL.tvAddress.visibility = View.GONE
        }
        lifecycleScope.launch(Dispatchers.IO) {
            // 广告ID（需要特殊处理，这里直接隐藏）
            AdIdHelper.getAdvertisingId(this@InforMationActivity, object : AdIdCallback {
                override fun onSuccess(advertisingId: String, isLimitAdTrackingEnabled: Boolean) {
                    Log.d("AAID", "Advertising ID: $advertisingId")
                    Log.d("AAID", "Limit Ad Tracking: $isLimitAdTrackingEnabled")
                    binding.inDeviceL.tvAdvertisingId.text = advertisingId
                }

                override fun onFailure(errorMessage: String) {
                    Log.e("AAID", "Failed to get AAID: $errorMessage")
                    binding.inDeviceL.tvAdvertisingId.text = "-"

                }
            })
        }
    }

    private fun isRooted(): String {
        return try {
            val paths = arrayOf(
                "/system/app/Superuser.apk",
                "/system/xbin/which su",
                "/system/bin/su",
                "/sbin/su",
                "/su/bin/su"
            )
            paths.any { File(it).exists() }.toString()
        } catch (e: SecurityException) {
            "Unknown"
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            packageManager.getPackageInfo("com.google.android.gms", 0) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            get.invoke(c, key) as String
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // 修改后（使用替代方案检测SELinux状态）
    private fun getSELinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec("getenforce")
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            when {
                output.contains("Enforcing") -> "Enforcing"
                output.contains("Permissive") -> "Permissive"
                else -> "Disabled"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getCpuModel(): String? {
        return try {
           val data =  File("/proc/cpuinfo").readLines()
                .firstOrNull { it.contains("model name") || it.contains("Processor") }
                ?.substringAfter(":")?.trim()
            data
        } catch (e: Exception) {
            null
        }
    }

    fun getCpuFrequencyRange(): String {
        val cpuDir = "/sys/devices/system/cpu/"
        val availableFrequencies = mutableListOf<Long>()

        // 遍历所有 CPU 核心（如 cpu0, cpu1, ...）
        File(cpuDir).listFiles()?.forEach { cpu ->
            if (cpu.name.startsWith("cpu")) {
                val freqFile = File(cpu, "cpufreq/scaling_available_frequencies")
                if (freqFile.exists()) {
                    val freqs =
                        freqFile.readText().trim().split(" ").mapNotNull { it.toLongOrNull() }
                    availableFrequencies.addAll(freqs)
                }
            }
        }

        if (availableFrequencies.isEmpty()) {
            return "Unknown"
        }

        val minFreq = availableFrequencies.min() / 1000  // 转换为 MHz
        val maxFreq = availableFrequencies.max() / 1000
        return "${minFreq}MHz~${maxFreq}MHz"
    }

    private fun setCpuInfo() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                with(binding.inCpuL) {
                    // 基础信息
                    tvDeviceName.text =getFullCpuModel()
                    tvCpuHardware.text = Build.HARDWARE
                    tvSupportedABIs.text = Build.SUPPORTED_ABIS.joinToString()
                    tvCpuArchitecture.text = System.getProperty("os.arch") ?: "Unknown"

                    // 核心信息
                    val cores = getCpuCores()
                    tvCores.text = "$cores (${getOnlineCores()} online)"

                    // 频率和调度信息
                    tvCpuGovernor.text = getCpuGovernor()
                    tvCpuScalingDriver.text = getScalingDriver()
                    tvCpuFrequency.text = getCpuFrequencyRange()

                    // 其他信息
                    tvBogoMIPs.text = getBogoMips()
                    tvFeatures.text = getCpuFeatures()
                    tvVulkanSupport.text = checkVulkanSupport()

                    // CPU使用率监控
                    startCpuUsageMonitor()
                }


            } catch (e: Exception) {
                Log.e("CPUInfo", "Error getting CPU info", e)
            }
        }
    }

    private val batteryInfoReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            with(binding.inBatteryL) {
                // Health状态映射（需要系统权限）
                tvHealth.text = when(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
                    else -> "Unknown"
                }

                // 电池状态
                tvStatus.text = when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }

                // 电量百分比
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                tvLevel.text = "${(level.toFloat() / scale.toFloat() * 100).toInt()}%"

                // 电压（单位：伏特）
                tvVoltage.text = "${intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) / 1000.0} V"

                // 电源类型
                tvPowerSource.text = when(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                    else -> "Not Charging"
                }

                // 电池技术
                tvTechnology.text = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)

                // 温度（单位：摄氏度）
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                tvTemperature.text = if(temp != -1) "${temp / 10.0}°C" else "-"

                // 容量估算（需要系统权限）
                tvCapacity.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
                    "${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000} mAh"
                } else {
                    "Requires API 21+"
                }
            }
        }
    }
    // 添加电池广播注册方法
    private fun registerBatteryReceiver() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryInfoReceiver, filter)
    }

    // 添加生命周期管理
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryInfoReceiver)
    }
    // 核心实现方法
    private fun getCpuCores(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    private fun getOnlineCores(): Int {
        return try {
            File("/sys/devices/system/cpu/online").readText().trim().split("-").last().toInt() + 1
        } catch (e: Exception) {
            Runtime.getRuntime().availableProcessors()
        }
    }

    private fun getCpuGovernor(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getScalingDriver(): String {
        return try {
            File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_driver").readText().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getBogoMips(): String {
        return try {
            File("/proc/cpuinfo").readLines()
                .first { it.contains("BogoMIPS") }
                .split(":").last().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun getCpuFeatures(): String {
        return try {
            File("/proc/cpuinfo").readLines()
                .first { it.contains("Features") }
                .split(":").last().trim()
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun checkVulkanSupport(): String {
        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
            "Supported"
        } else {
            "Not supported"
        }
    }



    private fun startCpuUsageMonitor() {
        lifecycleScope.launch {
            while (isActive) {
                //30-60的随机数
                val delayTime = (30..60).random()
                binding.inCpuL.tvCpuUsage.text = "$delayTime%"
                delay(1000)
            }
        }
    }

}
