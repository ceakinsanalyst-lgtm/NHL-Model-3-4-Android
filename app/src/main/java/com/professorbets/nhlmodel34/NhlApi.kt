package com.professorbets.nhlmodel34

import org.json.JSONObject

object NhlApi {
    fun schedule(date: String): List<NhlGame> {
        val root = JSONObject(Net.get("https://api-web.nhle.com/v1/schedule/$date"))
        val week = root.optJSONArray("gameWeek") ?: return emptyList()
        val out = mutableListOf<NhlGame>()
        for (i in 0 until week.length()) {
            val day = week.getJSONObject(i)
            val games = day.optJSONArray("games") ?: continue
            for (j in 0 until games.length()) {
                val g = games.getJSONObject(j)
                if (g.optInt("gameType", 2) != 2) continue
                val gameDate = g.optString("gameDate")
                if (gameDate != date) continue
                val h = g.getJSONObject("homeTeam")
                val a = g.getJSONObject("awayTeam")
                out += NhlGame(
                    id = g.getLong("id"),
                    gameDate = gameDate,
                    startTimeUtc = g.optString("startTimeUTC"),
                    homeAbbrev = h.optString("abbrev"),
                    awayAbbrev = a.optString("abbrev"),
                    homeName = h.optJSONObject("placeName")?.optString("default") ?: h.optString("abbrev"),
                    awayName = a.optJSONObject("placeName")?.optString("default") ?: a.optString("abbrev"),
                    gameState = g.optString("gameState")
                )
            }
        }
        return out.distinctBy { it.id }
    }

    fun currentSeasonGames(team: String, seasonId: String): List<JSONObject> {
        val root = JSONObject(Net.get("https://api-web.nhle.com/v1/club-schedule-season/$team/$seasonId"))
        val games = root.optJSONArray("games") ?: return emptyList()
        return (0 until games.length()).map { games.getJSONObject(it) }
    }
}
