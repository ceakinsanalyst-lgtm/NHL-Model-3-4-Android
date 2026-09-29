package com.professorbets.nhlmodel34

import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

object EloRepository {
    private const val K = 18.0
    private const val HOME_ADV = 35.0

    fun currentRatings(teamsNeeded: Set<String>, targetDate: LocalDate): Map<String,Double> {
        if (teamsNeeded.isEmpty()) return emptyMap()
        val seasonStart = if (targetDate.monthValue >= 7) targetDate.year else targetDate.year-1
        val seasonId = "%04d%04d".format(seasonStart,seasonStart+1)

        // Use every relevant team's season schedule, dedupe games, then replay in date order.
        // Starting at 1500 each season is a practical live approximation. V3.4's historical
        // research Elo carried across seasons; the UI labels this as a live Elo approximation.
        val allGames = mutableMapOf<Long, JSONObject>()
        val leagueSeeds = teamsNeeded.toMutableSet()
        // Pull schedules for today's teams first; their opponents are embedded, and deduping keeps requests modest.
        teamsNeeded.forEach { t ->
            NhlApi.currentSeasonGames(t,seasonId).forEach { g ->
                val gd = g.optString("gameDate")
                if (gd.isBlank() || !LocalDate.parse(gd).isBefore(targetDate)) return@forEach
                if (g.optInt("gameType",2)!=2) return@forEach
                if (g.optString("gameState") !in setOf("OFF","FINAL")) return@forEach
                allGames[g.getLong("id")]=g
                leagueSeeds += g.getJSONObject("homeTeam").optString("abbrev")
                leagueSeeds += g.getJSONObject("awayTeam").optString("abbrev")
            }
        }
        val ratings=leagueSeeds.associateWith{1500.0}.toMutableMap()
        allGames.values.sortedWith(compareBy<JSONObject>{it.optString("gameDate")}.thenBy{it.getLong("id")}).forEach { g ->
            val h=g.getJSONObject("homeTeam"); val a=g.getJSONObject("awayTeam")
            val ht=h.optString("abbrev"); val at=a.optString("abbrev")
            val hs=h.optInt("score",-1); val as_=a.optInt("score",-1)
            if (hs<0 || as_<0 || hs==as_) return@forEach
            val rh=ratings.getOrDefault(ht,1500.0); val ra=ratings.getOrDefault(at,1500.0)
            val eh=1.0/(1.0+10.0.pow(-((rh+HOME_ADV)-ra)/400.0))
            val sh=if(hs>as_)1.0 else 0.0
            ratings[ht]=rh+K*(sh-eh)
            ratings[at]=ra+K*((1.0-sh)-(1.0-eh))
        }
        return ratings
    }

    fun isBackToBack(team:String,targetDate:LocalDate):Boolean {
        val seasonStart=if(targetDate.monthValue>=7)targetDate.year else targetDate.year-1
        val seasonId="%04d%04d".format(seasonStart,seasonStart+1)
        val prior=NhlApi.currentSeasonGames(team,seasonId)
            .mapNotNull{it.optString("gameDate").takeIf(String::isNotBlank)?.let(LocalDate::parse)}
            .filter{it.isBefore(targetDate)}
            .maxOrNull() ?: return false
        return ChronoUnit.DAYS.between(prior,targetDate) <= 1
    }
}
