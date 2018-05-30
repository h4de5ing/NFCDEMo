package com.code19.nfcdemo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

open class MainActivity : AppCompatActivity() {
    private var mPendingIntent: PendingIntent? = null
    private var mNFCAdapter: NfcAdapter? = null
    var mHandler = Handler {
        when (it.what) {
            0 -> textView.text = it.obj.toString()
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mNFCAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNFCAdapter != null) {
            if (!mNFCAdapter!!.isEnabled) {
                startActivityForResult(Intent(Settings.ACTION_WIRELESS_SETTINGS), 0)
            } else {
                mPendingIntent = PendingIntent.getBroadcast(this, 0, Intent("NFC_TEST"), PendingIntent.FLAG_UPDATE_CURRENT)
                registerReceiver(broad, IntentFilter("NFC_TEST"))
            }
        }
    }

    private val broad = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            processData(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(broad)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun processData(intent: Intent) {
        val tagFromIntent = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tagFromIntent != null) {
            textView.append("\nId:" + DataUtils.bytesToHexString(tagFromIntent.id, true) + "\n TechList:" + Arrays.toString(tagFromIntent.techList))
        }
        try {
            val mfc = MifareClassic.get(tagFromIntent)
            if (mfc != null) {
                var auth = false
                var metaInfo = ""
                mfc.connect()
                val type = mfc.type//获取TAG的类型
                val sectorCount = mfc.sectorCount//获取TAG中包含的扇区数
                var typeS = ""
                when (type) {
                    MifareClassic.TYPE_CLASSIC -> typeS = "TYPE_CLASSIC"
                    MifareClassic.TYPE_PLUS -> typeS = "TYPE_PLUS"
                    MifareClassic.TYPE_PRO -> typeS = "TYPE_PRO"
                    MifareClassic.TYPE_UNKNOWN -> typeS = "TYPE_UNKNOWN"
                }
                metaInfo += ("卡片类型：" + typeS + "\n共" + sectorCount + "个扇区\n共" + mfc.blockCount + "个块\n存储空间: " + mfc.size + "B\n")
                for (j in 0 until sectorCount) {
                    auth = mfc.authenticateSectorWithKeyA(j, MifareClassic.KEY_DEFAULT)
                    val bCount: Int
                    var bIndex: Int
                    if (auth) {
                        metaInfo += "Sector $j:验证成功\n"
                        // 读取扇区中的块
                        bCount = mfc.getBlockCountInSector(j)
                        bIndex = mfc.sectorToBlock(j)
                        for (i in 0 until bCount) {
                            val data = mfc.readBlock(bIndex)
                            metaInfo += ("Block " + bIndex + " : " + DataUtils.saveHex2String(data) + "\n")
                            bIndex++
                        }
                    } else {
                        metaInfo += "Sector $j:验证失败\n"
                    }
                }
                val message = Message()
                message.what = 0
                message.obj = metaInfo
                mHandler.sendMessage(message)
                println("nfc 数据:$metaInfo")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("gh0st", "nfc onPause")
        if (mNFCAdapter != null) {
            try {
                mNFCAdapter!!.disableForegroundDispatch(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("gh0st", "nfc onResume")
        if (mNFCAdapter != null) {
            try {
                mNFCAdapter!!.enableForegroundDispatch(this, mPendingIntent, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
