package com.professorbets.nhlmodel34

import java.time.LocalDate

object MoneyPuckRepository {
    private val cache = mutableMapOf<String, TeamForm>()
    private val rawCsvCache = mutableMapOf<String, String>()

    private fun mpTeam(abbrev: String): String = when(abbrev) {
        "LAK" -> "L.A"
        "NJD" -> "N.J"
        "SJS" -> "S.J"
        "TBL" -> "T.B"
        else -> abbrev
    }

    fun teamForm(team: String, beforeDate: LocalDate, elo: Double, b2b: Boolean): TeamForm {
        val key = "$team|$beforeDate|$elo|$b2b"
        cache[key]?.let { return it }
        val csv = rawCsvCache.getOrPut(team) { Net.get("https://moneypuck.com/moneypuck/playerData/careers/gameByGame/regular/teams/${mpTeam(team)}.csv") }
        val lines = csv.lineSequence().iterator()
        if (!lines.hasNext()) throw IllegalStateException("Empty MoneyPuck file for $team")
        val header = Csv.parseLine(lines.next())
        val ix = header.withIndex().associate { it.value to it.index }
        fun idx(name: String)=ix[name] ?: error("Missing MoneyPuck column $name")

        data class Row(val date: LocalDate, val situation:String, val xgPct:Double?, val corsiPct:Double?, val gsax60:Double?)
        val rows = ArrayList<Row>()
        while (lines.hasNext()) {
            val p = Csv.parseLine(lines.next())
            if (p.size < header.size) continue
            val pos = p[idx("position")]
            if (pos != "Team Level") continue
            val ds = p[idx("gameDate")]
            if (ds.length != 8) continue
            val d = LocalDate.of(ds.substring(0,4).toInt(), ds.substring(4,6).toInt(), ds.substring(6,8).toInt())
            if (!d.isBefore(beforeDate)) continue
            val sit = p[idx("situation")]
            if (sit != "5on5" && sit != "all") continue
            fun dbl(name:String)=p[idx(name)].toDoubleOrNull()
            val xga=dbl("xGoalsAgainst")
            val ga=dbl("goalsAgainst")
            val ice=dbl("iceTime")
            val gsax60 = if (sit=="all" && xga!=null && ga!=null && ice!=null && ice>0) (xga-ga)*3600.0/ice else null
            rows += Row(d,sit,dbl("xGoalsPercentage"),dbl("corsiPercentage"),gsax60)
        }
        val sorted = rows.sortedBy { it.date }
        val ev = sorted.filter { it.situation=="5on5" }
        val all = sorted.filter { it.situation=="all" }
        fun avgLast(vals: List<Double?>, n:Int, fallback:Double) = vals.mapNotNull{it}.takeLast(n).let { if (it.isEmpty()) fallback else it.average() }
        val form=TeamForm(
            team=team,
            elo=elo,
            b2b=b2b,
            xg20=avgLast(ev.map{it.xgPct},20,.5),
            corsi20=avgLast(ev.map{it.corsiPct},20,.5),
            xg10=avgLast(ev.map{it.xgPct},10,.5),
            gsax20=avgLast(all.map{it.gsax60},20,0.0),
            gamesUsed=ev.size.coerceAtMost(20)
        )
        cache[key]=form
        return form
    }
}
