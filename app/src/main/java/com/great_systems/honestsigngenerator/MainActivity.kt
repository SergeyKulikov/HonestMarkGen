package com.great_systems.honestsigngenerator


import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import com.squareup.picasso.Picasso
import net.logistinweb.liw3.barcode.HonestSignDecoder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet


class MainActivity : AppCompatActivity() {
    private lateinit var gtins: AutoCompleteTextView
    private var gtinList: HashSet<String> = hashSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val dateformat = SimpleDateFormat("HHmmss", Locale.getDefault())
        var mark: String = ""
        val markList = StringBuilder()

        //val gtins: TextView = findViewById(R.id.ed_gtins)
        gtins = findViewById(R.id.ed_gtins)
        val result: TextView = findViewById(R.id.ed_marks)
        val button: Button = findViewById(R.id.btn_gen)
        val save: Button = findViewById(R.id.btn_save)
        val imageView: ImageView = findViewById(R.id.iv_mark)

        gtins.setText("9780201379624")
        val url1 = "https://barcode.tec-it.com/barcode.ashx?data="
        val url2 = "&code=GS1DataMatrix&translate-esc=on&dmsize=Default"

        setAdapter()
        loadGtins()

        button.setOnClickListener {
            val gtin_list = gtins.text.toString().split(',', ' ')
            var res = ""
            gtin_list.forEach { gtin ->
                var gtin1 = gtin
                if (gtin.length < 14) {
                    gtin1 = "00000000000000".substring(0, 14 - gtin.length) + gtin
                }

                val sn = genLine(13)
                val ky = genLine(4)
                val ac = genLine(41)
                Log.d("LN", "sn: $sn")

                mark = // HonestSignDecoder.data_separator+
                    "01" + gtin1 +
                            "21" + sn +
                            HonestSignDecoder.data_separator +
                            "91" + ky +
                            HonestSignDecoder.data_separator + "92" + ac

                res += mark + "\n"

                markList.append(res)

                if (!HonestSignDecoder().isHonestSight(mark)) {
                    Log.e("ERR", "это не марка!")
                } else {
                    Picasso.with(this@MainActivity)
                        .load(url1 + mark + url2)
                        // .placeholder(R.drawable.user_placeholder)
                        // .error(R.drawable.user_placeholder_error)
                        .into(imageView)


                }

            }
            result.text = res
            saveGtins()
        }

        save.setOnClickListener {
            //imageView.buildDrawingCache()
            // val bm: Bitmap = imageView.getDrawingCache()
            val bmp = imageView.drawToBitmap()
            saveImage(this, bmp, "mark_"+gtins.text+"_"+ dateformat.format(System.currentTimeMillis()))
            saveFile(this, markList.toString())

        }
    }


    fun saveImage(context: Context, bitmap: Bitmap, slug: String) {
        val newWidth = bitmap.width / 4
        val newHeight = bitmap.height / 4

        val bitmap = Bitmap.createScaledBitmap(
            bitmap, newWidth, newHeight, false
        )

        val contextWrapper = ContextWrapper(context)
        val directory: File =
            contextWrapper.getDir("customDirectory", Context.MODE_PRIVATE)
        val file = File(directory, slug + ".jpg")

        var outputStream: FileOutputStream? = null

        try {
            outputStream = FileOutputStream(file, true)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                outputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun saveFile(context: Context, mark: String) {
        val contextWrapper = ContextWrapper(context)
        val directory: File =
            contextWrapper.getDir("customDirectory", Context.MODE_PRIVATE)
        val file = File(directory, "mark_list.txt")

        var outputStream: FileOutputStream? = null

        try {
            outputStream = FileOutputStream(file)
            outputStream.write(mark.toByteArray(UTF_8))
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                outputStream!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private lateinit var adapter: ArrayAdapter<String>
    fun setAdapter() {
        val type: Array<String> = gtinList.toTypedArray()

        adapter = ArrayAdapter<String>(
            this,
            com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
            type
        )

        gtins.setAdapter(adapter)
    }

    fun loadGtins() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        sp.getStringSet("GTIN_LIST", setOf())?.let {
            gtinList.addAll(it)
        }

        setAdapter()
    }

    fun saveGtins() {
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        gtinList.add(gtins.text.toString())

        sp.edit().putStringSet("GTIN_LIST", gtinList).apply()

        loadGtins()
    }


    /*
    GTIN - штрихкод код товара, может соотвествовать ENA7,8,14 и т.д. но для формата GS1 DataMatrix
    * его длина всегда 14 симвовлов
    *
    * SERIAL - серийный номер товара. Он состоит из 13 символов: цифр, строчных и прописных
    * латинских букв и других специальных знаков.
    *
    * <KEY> - порядковый номер ключа проверки, который содержит 4 символа. Ему предшествует
    * идентификатор применения 91, а завершает символ-разделитель.
    *
    * <ACODE> — (authentication code) код проверки подлинности, включающий 44 символа с размещенным
    * впереди идентификатором применения 92.

    <FNC1> 01 <GTIN> 21 <SERIAL> <GS> 91 <KEY> <GS> 92 <ACODE>

    */

    val smb: String = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM~!%_-+[]"

    private fun genLine(len: Int): String {
        var result = ""
        for (i in 0 until len) {
            val idx = (Math.random() * smb.length).toInt()
            result += smb.substring(idx, idx + 1)
        }
        return result
    }
}