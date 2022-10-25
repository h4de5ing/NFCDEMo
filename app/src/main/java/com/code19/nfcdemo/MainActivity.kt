package com.code19.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Build
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
                val filter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
                    try {
                        addDataType("*/*")    /* Handles all MIME based dispatches.You should specify only the ones that you need. */
                    } catch (e: IntentFilter.MalformedMimeTypeException) {
                        throw RuntimeException("fail", e)
                    }
                }
                mFilters = arrayOf(filter)
                mTechLists = arrayOf(arrayOf(NfcA::class.java.name))
            }
        } else updateTv("当前设备不支持NFC！")
    }

    override fun onPause() {
        super.onPause()
        mNFCAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        mNFCAdapter?.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists)
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
            //支持的标签技术
            val nfcA = NfcA.get(tag)
            val nfcB = NfcB.get(tag)
            val nfcF = NfcF.get(tag)
            val nfcV = NfcV.get(tag)
            val isoDep = IsoDep.get(tag)
            val ndef = Ndef.get(tag)
            val ndefFormatable = NdefFormatable.get(tag)
            if (Build.VERSION.SDK_INT >= 17) {
                val nfcBarcode = NfcBarcode.get(tag)
            }
            //可选择支持的标签技术
            val mfc = MifareClassic.get(tag)
            val ultralight = MifareUltralight.get(tag)
            if (isoDep != null) {
                updateTv("▶ IsoDep Maximum transceive length: ${isoDep.maxTransceiveLength} bytes")
                updateTv("▶ IsoDep Default maximum transceive time-out: ${isoDep.timeout}ms")
                if (Build.VERSION.SDK_INT >= 16)
                    updateTv("▶ IsoDep Extended length APDUs ${if (isoDep.isExtendedLengthApduSupported) "" else "not"} supported")
                updateTv("\n")
            }
            if (nfcA != null) {
                updateTv("▶ NfcA ATQA: ${DataUtils.bytesToHexString(nfcA.atqa, true)}")
                updateTv("▶ NfcA SAK: 0x${nfcA.sak.toString(16)}")
                updateTv("▶ NfcA Maximum transceive length: ${nfcA.maxTransceiveLength} bytes")
                updateTv("▶ NfcA Default maximum transceive time-out: ${nfcA.timeout}ms")
                updateTv("\n")
            }

            if (mfc != null) {
                var metaInfo = ""
                mfc.connect()
                //mfc.timeout = 2000
                updateTv("▶ MifareClassic Maximum transceive length: ${mfc.maxTransceiveLength} bytes")
                updateTv("▶ MifareClassic Default maximum transceive time-out: ${mfc.timeout}ms")
                var auth: Boolean
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
                        metaInfo += String.format(
                            "Sector %d (0x%02X)\n",
                            Integer.valueOf(j),
                            Integer.valueOf(j)
                        )
                        // 读取扇区中的块
                        bCount = mfc.getBlockCountInSector(j)
                        bIndex = mfc.sectorToBlock(j)
                        for (i in 0 until bCount) {
                            val data = mfc.readBlock(bIndex)
                            metaInfo += ("Block " + String.format(
                                "[%02X] ",
                                Integer.valueOf(bIndex)
                            ) + " : " + DataUtils.saveHex2String(data) + "\n")
                            bIndex++
                        }
                        metaInfo += "\n"
                    }
                }
                updateTv(metaInfo)
                mfc.close()
            }
            if (ndef != null) {
                updateTv("读取 Ndef")
                ndef.connect()
                updateTv(if (ndef.isWritable) "可读" else "不可读")
                updateTv("容量：${ndef.maxSize}")
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
                val payload = ultralight.readPages(4)
                updateTv(String(payload, Charset.forName("US-ASCII")))
                ultralight.close()
            }
        } catch (e: Exception) {
            updateTv("NFC读取错误 ${e.message}")
            e.printStackTrace()
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
