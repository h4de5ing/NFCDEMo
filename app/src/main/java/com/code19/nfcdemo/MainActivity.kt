package com.code19.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Bundle
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.charset.Charset
import java.util.*
import kotlin.experimental.and


open class MainActivity : AppCompatActivity() {
    private var mPendingIntent: PendingIntent? = null
    private var mNFCAdapter: NfcAdapter? = null
    private var mFilters: Array<IntentFilter>? = null
    private var mTechLists: Array<Array<String>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView.movementMethod = ScrollingMovementMethod()
        clean.setOnClickListener { textView.text = "" }
        mNFCAdapter = NfcAdapter.getDefaultAdapter(this)
        if (mNFCAdapter != null) {
            if (!mNFCAdapter!!.isEnabled) {
                startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } else {
                mPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    0
                )
                val filter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
                mFilters = arrayOf(filter)
                mTechLists = arrayOf(arrayOf(NfcA::class.java.name))
            }
        } else updateTv("当前设备不支持NFC！")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        processData(intent)
    }

    private fun processData(intent: Intent) {
        updateTv("发现新的TAG: ${intent.action}")
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            updateTv(
                "\nID:" + DataUtils.bytesToHexString(
                    tag.id,
                    true
                ) + "\nTAG Tech:" + Arrays.toString(tag.techList)
            )
        }
        try {
            val mfc = MifareClassic.get(tag)
            val isoDep = IsoDep.get(tag)
            val ndef = Ndef.get(tag)
            val ultralight = MifareUltralight.get(tag)
            if (mfc != null) {
                updateTv("读取 MifareClassic")
                var auth: Boolean
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
                updateTv(metaInfo)
                updateTv("nfc 数据:$metaInfo")
                mfc.close()
            }
            if (isoDep != null) {
                updateTv("读取 IsoDep")
            }
            if (ndef != null) {
                updateTv("读取 Ndef")
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                ndefMessage.records.forEachIndexed { index, it ->
                    if (it.tnf != NdefRecord.TNF_WELL_KNOWN) updateTv("数据为非文本格式数据")
                    if (!Arrays.equals(it.type, NdefRecord.RTD_TEXT)) updateTv("非文本格式数据")
                    //updateTv("ndef Payload ${index}: $it")
                    //获得字节流
                    val payload = it.payload
                    // 获得编码格式
                    val textEncoding: String =
                        if ((payload[0] and 0x80.toByte()) == 0x00.toByte()) "utf-8" else "utf-16"
                    // 获得语言编码长度
                    val languageCodeLength = (payload[0] and 0x3f).toInt()
                    // 语言编码
                    val languageCode =
                        String(payload, 1, languageCodeLength, Charset.forName("US-ASCII"))
                    // 获取文本
                    updateTv(
                        String(
                            payload,
                            languageCodeLength + 1,
                            payload.size - languageCodeLength - 1,
                            Charset.forName(textEncoding)
                        )
                    )
                }
                ndef.close()
            }
            if (ultralight != null) {
                updateTv("读取 MifareUltralight")
                ultralight.connect()
                val bytes = ultralight.readPages(4)
                updateTv(String(bytes))
                ultralight.close()
            }
        } catch (e: Exception) {
            updateTv("NFC读取错误 ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
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
        if (mNFCAdapter != null) {
            try {
                mNFCAdapter!!.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateTv(message: String) {
        try {
            runOnUiThread {
                textView?.apply {
                    this.append("\n${message}")
                    val offset: Int = this.lineCount * this.lineHeight - this.height
                    this.scrollTo(0, offset.coerceAtLeast(0))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
