package com.example.resumematcher

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object DocumentParser {

    fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = cursor.getString(index)
                }
            }
        }
        if (name.isEmpty()) {
            name = uri.path?.substringAfterLast('/') ?: "unknown"
        }
        return name
    }

    fun getFileSizeString(context: Context, uri: Uri): String {
        var size: Long = -1
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    size = cursor.getLong(index)
                }
            }
        }
        if (size == -1L) return "unknown size"
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${String.format("%.1f", size / 1024.0)} KB"
            else -> "${String.format("%.1f", size / (1024.0 * 1024.0))} MB"
        }
    }

    fun extractText(context: Context, uri: Uri): String {
        val fileName = getFileName(context, uri).lowercase()
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri) ?: throw Exception("Failed to open file stream")
        
        return try {
            when {
                fileName.endsWith(".pdf") -> {
                    extractTextFromPdf(inputStream)
                }
                fileName.endsWith(".docx") -> {
                    extractTextFromDocx(inputStream)
                }
                fileName.endsWith(".doc") -> {
                    extractTextFromDoc(inputStream)
                }
                fileName.endsWith(".txt") -> {
                    extractTextFromTxt(inputStream)
                }
                else -> {
                    // Fallback: try to see if we can parse as TXT
                    try {
                        extractTextFromTxt(inputStream)
                    } catch (e: Exception) {
                        throw Exception("Unsupported file format. Please upload PDF, DOC, or DOCX.")
                    }
                }
            }
        } finally {
            try {
                inputStream.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun extractTextFromPdf(inputStream: InputStream): String {
        var reader: PdfReader? = null
        try {
            reader = PdfReader(inputStream)
            val textBuilder = StringBuilder()
            val pages = reader.numberOfPages
            for (i in 1..pages) {
                val strategy = LocationTextExtractionStrategy()
                val pageText = PdfTextExtractor.getTextFromPage(reader, i, strategy)
                if (pageText != null) {
                    textBuilder.append(pageText).append("\n")
                }
            }
            return textBuilder.toString().trim()
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun extractTextFromDocx(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        val textBuilder = StringBuilder()
        var entry = zipInputStream.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val reader = BufferedReader(InputStreamReader(zipInputStream, "UTF-8"))
                var line = reader.readLine()
                while (line != null) {
                    var index = 0
                    while (true) {
                        val startTag = line.indexOf("<w:t", index)
                        if (startTag == -1) break
                        val closingBracket = line.indexOf(">", startTag)
                        if (closingBracket == -1) break
                        val endTag = line.indexOf("</w:t>", closingBracket)
                        if (endTag == -1) break
                        val text = line.substring(closingBracket + 1, endTag)
                        textBuilder.append(text).append(" ")
                        index = endTag + 6
                    }
                    line = reader.readLine()
                }
                break
            }
            entry = zipInputStream.nextEntry
        }
        return textBuilder.toString()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractTextFromDoc(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        val textBuilder = StringBuilder()
        val n = bytes.size
        
        // Scan for UTF-16LE characters (characters between 0x20 and 0x7E / tabs / newlines)
        var utf16Text = StringBuilder()
        var i = 0
        while (i < n - 1) {
            val b1 = bytes[i].toInt() and 0xFF
            val b2 = bytes[i + 1].toInt() and 0xFF
            if (b2 == 0 && (b1 == 9 || b1 == 10 || b1 == 13 || (b1 in 32..126) || (b1 in 160..255))) {
                utf16Text.append(b1.toChar())
                i += 2
            } else {
                if (utf16Text.length >= 4) {
                    textBuilder.append(utf16Text).append(" ")
                }
                utf16Text.setLength(0)
                i += 2
            }
        }
        if (utf16Text.length >= 4) {
            textBuilder.append(utf16Text).append(" ")
        }
        
        // Scan for ASCII characters
        var asciiText = StringBuilder()
        i = 0
        while (i < n) {
            val b = bytes[i].toInt() and 0xFF
            if (b == 9 || b == 10 || b == 13 || (b in 32..126)) {
                asciiText.append(b.toChar())
            } else {
                if (asciiText.length >= 4) {
                    textBuilder.append(asciiText).append(" ")
                }
                asciiText.setLength(0)
            }
            i++
        }
        if (asciiText.length >= 4) {
            textBuilder.append(asciiText).append(" ")
        }
        
        return textBuilder.toString()
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractTextFromTxt(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
        val textBuilder = StringBuilder()
        var line = reader.readLine()
        while (line != null) {
            textBuilder.append(line).append("\n")
            line = reader.readLine()
        }
        return textBuilder.toString().trim()
    }
}
