package com.thrustwealth.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.thrustwealth.app.data.model.CalculationRecord
import com.thrustwealth.app.data.model.Instrument
import com.thrustwealth.app.data.model.TradeEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportHelper {
    private const val DISCLAIMER =
        "For calculation purposes only. Not investment advice."

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    fun exportHistoryCsv(context: Context, records: List<CalculationRecord>) {
        val sb = StringBuilder()
        sb.appendLine("id,symbol,mode,balance,riskPercent,riskAmount,stopPips,entryPrice,direction,lotSize,pipValue,notionalValue,stopLossPrice,createdAt")
        records.forEach { r ->
            sb.appendLine(
                "${r.id},${r.symbol},${r.mode},${r.balance},${r.riskPercent},${r.riskAmount}," +
                    "${r.stopPips},${r.entryPrice},${r.direction},${r.lotSize},${r.pipValue}," +
                    "${r.notionalValue},${r.stopLossPrice ?: ""},${r.createdAt}"
            )
        }
        sb.appendLine()
        sb.appendLine(DISCLAIMER)
        shareFile(context, "history_${stamp()}.csv", sb.toString(), "text/csv")
    }

    fun exportTradesCsv(context: Context, trades: List<TradeEntry>) {
        val sb = StringBuilder()
        sb.appendLine("id,symbol,direction,lots,entryPrice,exitPrice,pnl,note,createdAt")
        trades.forEach { t ->
            sb.appendLine(
                "${t.id},${t.symbol},${t.direction},${t.lots},${t.entryPrice}," +
                    "${t.exitPrice},${t.pnl},\"${t.note.replace("\"", "\"\"")}\",${t.createdAt}"
            )
        }
        sb.appendLine()
        sb.appendLine(DISCLAIMER)
        shareFile(context, "journal_${stamp()}.csv", sb.toString(), "text/csv")
    }

    fun exportSpecsCsv(context: Context, instruments: List<Instrument>) {
        val sb = StringBuilder()
        sb.appendLine("id,name,category,contractSize,unit,pipSize,tickSize,digits,baseCurrency,quoteCurrency,defaultPrice,isFavorite,isCustom")
        instruments.forEach { i ->
            sb.appendLine(
                "${i.id},\"${i.name}\",${i.category},${i.contractSize},${i.unit}," +
                    "${i.pipSize},${i.tickSize},${i.digits},${i.baseCurrency},${i.quoteCurrency}," +
                    "${i.defaultPrice},${i.isFavorite},${i.isCustom}"
            )
        }
        sb.appendLine()
        sb.appendLine("Reference specs only. Actual contract specs may vary by broker. $DISCLAIMER")
        shareFile(context, "specs_${stamp()}.csv", sb.toString(), "text/csv")
    }

    fun exportSingleCalculationCsv(
        context: Context,
        symbol: String,
        mode: String,
        balance: Double,
        riskPercent: Double,
        riskAmount: Double,
        stopPips: Double,
        lotSize: Double,
        pipValue: Double,
        notional: Double
    ) {
        val sb = StringBuilder()
        sb.appendLine("symbol,mode,balance,riskPercent,riskAmount,stopPips,lotSize,pipValue,notionalValue")
        sb.appendLine("$symbol,$mode,$balance,$riskPercent,$riskAmount,$stopPips,$lotSize,$pipValue,$notional")
        sb.appendLine()
        sb.appendLine(DISCLAIMER)
        shareFile(context, "calc_${stamp()}.csv", sb.toString(), "text/csv")
    }

    fun exportPnlCsv(
        context: Context,
        symbol: String,
        direction: String,
        openPrice: Double,
        closePrice: Double,
        lots: Double,
        pnl: Double,
        pips: Double,
        returnPct: Double
    ) {
        val sb = StringBuilder()
        sb.appendLine("symbol,direction,openPrice,closePrice,lots,pnl,pips,returnPercent")
        sb.appendLine("$symbol,$direction,$openPrice,$closePrice,$lots,$pnl,$pips,$returnPct")
        sb.appendLine()
        sb.appendLine(DISCLAIMER)
        shareFile(context, "pnl_${stamp()}.csv", sb.toString(), "text/csv")
    }

    fun exportFullBackupJson(
        context: Context,
        history: List<CalculationRecord>,
        trades: List<TradeEntry>,
        instruments: List<Instrument>
    ) {
        val payload = buildString {
            appendLine("{")
            appendLine("  \"exportedAt\": \"${Date()}\",")
            appendLine("  \"disclaimer\": \"$DISCLAIMER\",")
            appendLine("  \"history\": ${historyToJson(history)},")
            appendLine("  \"trades\": ${tradesToJson(trades)},")
            appendLine("  \"customInstruments\": ${json.encodeToString(instruments.filter { it.isCustom })}")
            appendLine("}")
        }
        shareFile(context, "thrust_backup_${stamp()}.json", payload, "application/json")
    }

    fun shareText(context: Context, title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$body\n\n$DISCLAIMER")
        }
        context.startActivity(Intent.createChooser(intent, "Share").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun historyToJson(history: List<CalculationRecord>): String {
        return history.joinToString(prefix = "[", postfix = "]") { r ->
            """{"id":${r.id},"symbol":"${r.symbol}","mode":"${r.mode}","balance":${r.balance},"riskPercent":${r.riskPercent},"riskAmount":${r.riskAmount},"stopPips":${r.stopPips},"entryPrice":${r.entryPrice},"direction":"${r.direction}","lotSize":${r.lotSize},"createdAt":${r.createdAt}}"""
        }
    }

    private fun tradesToJson(trades: List<TradeEntry>): String {
        return trades.joinToString(prefix = "[", postfix = "]") { t ->
            """{"id":${t.id},"symbol":"${t.symbol}","direction":"${t.direction}","lots":${t.lots},"entryPrice":${t.entryPrice},"exitPrice":${t.exitPrice},"pnl":${t.pnl},"note":"${t.note.replace("\"", "\\\"")}","createdAt":${t.createdAt}}"""
        }
    }

    private fun shareFile(context: Context, fileName: String, content: String, mime: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName).apply { writeText(content) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun stamp(): String = dateFormat.format(Date())
}
