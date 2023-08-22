package net.logistinweb.liw3.barcode

import com.google.mlkit.vision.barcode.common.Barcode
import java.util.regex.Pattern
import java.lang.Exception

/**
 * Общепринятые разделители форматов данных
 *
 * 28 - FS - разделитель файлов Файл сепаратор FS - сейчас используется для воспроизведения в
 * случайном доступе, таком как RAM и магнитные диски. Это управляющий код для сигнала разделение
 * двух файлов.
 *
 * 29 - GS - разделитель групп. Групповой разделитель GS определяется для разделения таблиц в системе
 * хранения последовательных данных.
 *
 * 30 - RS - разделитель записи В группе (или таблице) записи разделены RS или записью разделитель.
 *
 * 31 - US - разделитель блока. Наименьшие данные, подлежащие хранению в базе данных называются
 * единицами в ASCII, сечас это поля в базе данных. Разделитель блока позволяет всем полям БД
 * иметь переменную длину для экономии места.
 *
 * 232 - FNC1 - начало формата GS1 DataMatrix для маркировки товара. В РФ после его введения
 * маркировки в этом формате постоянно делают ошибки при генерации и, как правило, заменяют его
 * на 29 (GS). Но сделаем чтобы и правильные форматы тоже вопринимались.
 **/

/**
 * Формат кодирования GS1 DataMatrix
 *
 * GTIN - штрихкод код товара, может соотвествовать ENA7,8,14 и т.д. но для формата GS1 DataMatrix
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
 *
 * GS1 DataMatrix == <FNC1> 01 <GTIN> 21 <SERIAL> <GS> 91 <KEY> <GS> 92 <ACODE>
 *
 */

class HonestSignDecoder {
    // https://www.gs1.org/standards/gs1-datamatrix-guideline/25#4-Symbol-marking-techniques+4-6-Verification-of-symbol-(data-and-print-quality)
    private var patternNum = -1;

    class GS1DataMatrixInfo {
        var dataMatrix: String? = null         // Это нельзя(?) хранитьв БД по законадательству РФ
        var dataMatrixDatabase: String? = null // Это можно сохранить в базе данных
        var matrixGroups = ArrayList<String>()

        var GTIN: String = ""
        var SERIAL: String = ""
        var KEY: String = ""
        var ACODE: String = ""

        override fun toString(): String {
            var str = ""
            for (i in 0 until matrixGroups.size) {
                str = str + i + ": " + matrixGroups[i] + "\n"
            }
            return str
        }

        fun getInfo(): String {
            return "GTIN: $GTIN\nSERIAL: $SERIAL\nKEY: $KEY\nAUTHENTICATION CODE: $ACODE"
        }
    }

    fun decode(barcode: Barcode): GS1DataMatrixInfo {
        // Просто расшифровали и вернули
        return parseGS1DataMatrixHonestSign(barcode.rawValue)
    }

    private fun decode(barcode: String): GS1DataMatrixInfo {
        // Просто расшифровали и вернули
        return parseGS1DataMatrixHonestSign(barcode)
    }

    private fun parseGS1DataMatrixHonestSign(barcodeRow: String?): GS1DataMatrixInfo {
        val data = GS1DataMatrixInfo()

        if (barcodeRow == null) {
            return data
        }
        data.dataMatrix = barcodeRow

        /*
        (01)04605025000880(3103)000830(17)161223

        Данные:

        ИП (01) обозначает, что далее идёт GTIN, 14 цифр
        ИП (3103) Вес нетто («3» указывает положение десятичной точки, 0 кг 830 г )
        ИП (17) Срок годности (ГГММДД)

        392Х	Цена
        310X	Вес, кг
        30	Количество
        10	Номер партии
        11	Дата производства
        15	Годен до/Продать до
        17	Срок годности
        422	Страна происхождения
        21	Серийный номер
        8008	Дата и время производства
        */

        val pattern_string: ArrayList<String> = ArrayList();
        pattern_string.add("${gs1_data_matrix_begin}01(\\d{14})21(\\S{13})${data_separator}91(\\S+)${data_separator}92(\\S+)") // правильный формат
        pattern_string.add("${data_separator}?01(\\d{14})21(\\S{13})${data_separator}91(\\S+)${data_separator}92(\\S+)") // неправильный начальный символ, но подойдет

        var i = 0;
        var result = false

        // Log.d("MARK", "$barcodeRow")
        while (!result && i < pattern_string.size) {
            result = parseGS1WithPattern(barcodeRow, pattern_string[i], data)
            // Log.d("MARK", "${i+1}) ${pattern_string[i]}: $result: ${barcodeRow[0].toInt()}")
            patternNum = i
            i++
        }

        return data
    }

    fun isAuthorizedGTIN(matrixInfo: GS1DataMatrixInfo, gtins: ArrayList<String>): Boolean {
        // Проверка есть ли в списке разрешенных GTIN тот, который закодирован в GS1 DataMatrix
        if (gtins.size == 1) {
            // разрешены все GTIN 03/10/2022
            if (gtins[0].trim().isEmpty()) {
                gtins.removeAt(0);
            }
        }

        if (gtins.size == 0) return true

        for (gtinTemplate in gtins) {
            if (gtinTemplate == matrixInfo.GTIN) {
                // сотвествем одному из списка рарешенных GTIN
                return true
            }
        }
        return false
    }

    fun isAuthorizedGTIN(barcode: String, gtins: ArrayList<String>): Boolean {
        return isAuthorizedGTIN(decode(barcode), gtins)
    }

    private fun parseGS1WithPattern(barcodeRow: String, patternString: String, data: GS1DataMatrixInfo): Boolean {
        var result = false

        try {
            val pattern = Pattern.compile(patternString, Pattern.MULTILINE)
            val matcher = pattern.matcher(barcodeRow)

            if (matcher.find())
                matcher.matches().apply {
                    result = this;
                    if (this) {
                        data.GTIN = matcher.group(1) as String
                        data.SERIAL = matcher.group(2) as String
                        data.KEY = matcher.group(3) as String
                        data.ACODE = matcher.group(4) as String
                    }
                    return result;
                }
        } catch (ignore: Exception) {
        }
        return result;
    }

    fun isHonestSight(barcode: String): Boolean {
        val matrixInfo: GS1DataMatrixInfo = decode(barcode);
        return (matrixInfo.GTIN.length == 14
                && matrixInfo.SERIAL.length == 13
                && matrixInfo.ACODE.isNotEmpty()
                && matrixInfo.KEY.isNotEmpty()
                );
    }

    fun isHonestSight(matrixInfo: GS1DataMatrixInfo): Boolean {
        return (matrixInfo.GTIN.length == 14
                && matrixInfo.SERIAL.length == 13
                && matrixInfo.ACODE.isNotEmpty()
                && matrixInfo.KEY.isNotEmpty()
                );
    }

    public fun getTriggeredPattern(): Int {
        // Какой паттерн сработал. Можно использовать для отображения ошибок кодирования GS1
        return patternNum;
    }

    companion object {
        const val component_separator = 31.toChar().toString()
        const val data_separator = 29.toChar().toString()
        const val segment_separator = 28.toChar().toString()
        const val gs1_data_matrix_begin = 232.toChar().toString()
    }
}