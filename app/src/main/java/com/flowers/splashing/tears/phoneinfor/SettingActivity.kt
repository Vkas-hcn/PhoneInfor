package com.flowers.splashing.tears.phoneinfor

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.view.animation.LinearInterpolator
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.flowers.splashing.tears.phoneinfor.databinding.ActivitySettingBinding
import com.flowers.splashing.tears.phoneinfor.databinding.ActivityStartBinding

class SettingActivity:AppCompatActivity() {
    private val binding by lazy { ActivitySettingBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.textView2.setOnClickListener {
            finish()
        }
        binding.tvShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "https://github.com/love-flower/WallPaperApp${this@SettingActivity.packageName}")
            startActivity(Intent.createChooser(intent, "Share on:"))
        }
        binding.tvPp.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            //TODO 跳转网页
            intent.data =
                android.net.Uri.parse("https://www.google.com")
            startActivity(intent)
        }
    }

}