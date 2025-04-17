package com.flowers.splashing.tears.phoneinfor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.text.format.Formatter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.flowers.splashing.tears.phoneinfor.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private lateinit var batteryReceiver: BatteryReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.conFirst)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置手机型号
        binding.tvPhoneModel.text = Build.BRAND + " "+Build.MODEL

        // 内存信息
        updateMemoryInfo()

        // 存储信息
        updateStorageInfo()
        clickFun()
    }

    private fun clickFun() {
        with(binding){
            imgSet.setOnClickListener {
                val intent = Intent(this@MainActivity, SettingActivity::class.java)
                startActivity(intent)
            }
            conDevice.setOnClickListener {
                val intent = Intent(this@MainActivity, InforMationActivity::class.java)
                intent.putExtra("intType", 1)
                startActivity(intent)
            }

            conSystem.setOnClickListener {
                val intent = Intent(this@MainActivity, InforMationActivity::class.java)
                intent.putExtra("intType", 2)
                startActivity(intent)
            }
            conCPU.setOnClickListener {
                val intent = Intent(this@MainActivity, InforMationActivity::class.java)
                intent.putExtra("intType", 3)
                startActivity(intent)
            }
            conBatterys.setOnClickListener {
                val intent = Intent(this@MainActivity, InforMationActivity::class.java)
                intent.putExtra("intType", 4)
                startActivity(intent)
            }
        }
    }

    private fun updateMemoryInfo() {
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager).getMemoryInfo(memoryInfo)

        val totalRam = memoryInfo.totalMem
        val availableRam = memoryInfo.availMem
        val usedRam = totalRam - availableRam
        val ramUsagePercent = (usedRam.toDouble() / totalRam.toDouble() * 100).toInt()

        with(binding) {
            tvRamPro.text = "$ramUsagePercent%"
            tvUsed.text = Formatter.formatFileSize(this@MainActivity, usedRam)
            tvRemain.text = Formatter.formatFileSize(this@MainActivity, availableRam)
            ramPro.setProgress(ramUsagePercent.toFloat())
        }
    }

    private fun updateStorageInfo() {
        // System Storage
        updateSystemStorage()

        // Internal Storage
        updateInternalStorage()
    }

    private fun updateSystemStorage() {
        val stat = StatFs(File("/system").absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val used = total - free
        val percent = (used.toDouble() / total.toDouble() * 100).toInt()

        with(binding) {
            tvSysPro.progress = percent
            tvSysProValue.text = "$percent%"
            textView5.text = "Free: ${Formatter.formatFileSize(this@MainActivity, free)}, Total: ${Formatter.formatFileSize(this@MainActivity, total)}"
        }
    }

    private fun updateInternalStorage() {
        val path = File("/data")
        val stat = StatFs(path.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val used = total - free
        val percent = (used.toDouble() / total.toDouble() * 100).toInt()

        with(binding) {
            tvIntPro.progress = percent
            tvIntProValue.text = "$percent%"
            tvIntValue.text = "Free: ${Formatter.formatFileSize(this@MainActivity, free)}, Total: ${Formatter.formatFileSize(this@MainActivity, total)}"
        }
    }

    override fun onResume() {
        super.onResume()
        // 注册电池状态接收器
        batteryReceiver = BatteryReceiver()
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // 注销接收器
        unregisterReceiver(batteryReceiver)
    }

    inner class BatteryReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // 获取电池信息
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

            // 转换温度单位（摄氏度）
            val tempCelsius = temperature / 10f

            // 转换电压单位（伏特）
            val voltageV = voltage / 1000f

            // 计算百分比
            val batteryPct = level * 100 / scale.toFloat()

            // 更新UI
            with(binding) {
                tvBatteryPro.progress = batteryPct.toInt()
                tvBatteryProValue.text = "${batteryPct.toInt()}%"
                tvBatteryValue.text = """
                    Voltage: ${"%.2f".format(voltageV)}V,Temp: ${"%.1f".format(tempCelsius)}°C
                """.trimIndent()
            }
        }
    }
}
