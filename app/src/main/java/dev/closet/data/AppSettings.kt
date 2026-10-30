package dev.closet.data

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

data class AppSettings(
    val theme: String = "system", val accent: String = "rose", val hemisphere: String = "north",
    val autoFill: Boolean = true, val autoRotate: Boolean = true, val cleanup: Boolean = true,
    val hatFrequency: Float = .4f, val jacketFrequency: Float = .5f, val repeatDays: Int = 7
) {
    fun season(date: LocalDate = LocalDate.now()): String {
        if(hemisphere=="tropical") return "all"
        val month=if(hemisphere=="south") (date.monthValue+5)%12+1 else date.monthValue
        return when(month) {3,4,5->"spring";6,7,8->"summer";9,10,11->"autumn";else->"winter"}
    }
    fun json() = JSONObject().put("theme",theme).put("accent",accent).put("hemisphere",hemisphere)
        .put("autoFill",autoFill).put("autoRotate",autoRotate).put("cleanup",cleanup)
        .put("hatFrequency",hatFrequency).put("jacketFrequency",jacketFrequency).put("repeatDays",repeatDays).toString()
    companion object {
        fun parse(s: String): AppSettings {
            val j=JSONObject(s)
            val theme=j.optString("theme","system");val accent=j.optString("accent","rose");val hemisphere=j.optString("hemisphere","north")
            require(theme in listOf("system","light","dark") && accent in listOf("rose","lilac","teal") && hemisphere in listOf("north","south","tropical")) { "Unsupported backup settings." }
            return AppSettings(theme,accent,hemisphere,j.optBoolean("autoFill",true),j.optBoolean("autoRotate",true),j.optBoolean("cleanup",true),
                j.optDouble("hatFrequency",.4).toFloat().coerceIn(0f,1f),j.optDouble("jacketFrequency",.5).toFloat().coerceIn(0f,1f),j.optInt("repeatDays",7).coerceIn(0,60))
        }
        fun load(context: Context) = runCatching { parse(context.getSharedPreferences("settings",0).getString("data","{}")!!) }.getOrDefault(AppSettings())
        fun save(context: Context, settings: AppSettings) { check(context.getSharedPreferences("settings",0).edit().putString("data",settings.json()).commit()) }
    }
}
