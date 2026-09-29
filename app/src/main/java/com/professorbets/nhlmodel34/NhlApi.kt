package com.professorbets.nhlmodel34

import org.json.JSONArray
import org.json.JSONObject

object NhlApi {

    fun schedule(date: String): List<NhlGame> {

        // First try the NHL score endpoint.
        val scoreGames = runCatching {
            val root = JSONObject(
                Net.get("https://api-web.nhle.com/v1/score/$date")
            )
            parseGames(root.optJSONArray("games"), date)
        }.getOrDefault(emptyList())

        if (scoreGames.isNotEmpty()) return scoreGames

        // Fallback to the league schedule endpoint.
        val root = JSONObject(
            Net.get("https://api-web.nhle.com/v1/schedule/$date")
        )

        val out = mutableListOf<NhlGame>()

        // Some NHL responses expose games directly.
        out += parseGames(root.optJSONArray("games"), date)

        // Schedule responses normally use gameWeek.
        val week = root.optJSONArray("gameWeek")
        if (week != null) {
            for (i in 0 until week.length()) {
                val day = week.getJSONObject(i)

                // Only use the requested date when supplied.
                val dayDate = day.optString("date")
                if (dayDate.isNotBlank() && dayDate != date) continue

                out += parseGames(day.optJSONArray("games"), date)
            }
        }

        return out.distinctBy { it.id }
    }

    private fun parseGames(
        games: JSONArray?,
        requestedDate: String
    ): List<NhlGame> {

        if (games == null) return emptyList()

        val out = mutableListOf<NhlGame>()

        for (i in 0 until games.length()) {
            val g = games.getJSONObject(i)

            // Accept preseason and regular-season games.
            val gameType = g.optInt("gameType", 2)
            if (gameType != 1 && gameType != 2) continue

            val gameDate = g.optString("gameDate", requestedDate)
            if (gameDate.isNotBlank() && gameDate != requestedDate) continue

            val h = g.optJSONObject("homeTeam") ?: continue
            val a = g.optJSONObject("awayTeam") ?: continue

            out += NhlGame(
                id = g.optLong("id"),
                gameDate = gameDate,
                startTimeUtc = g.optString("startTimeUTC"),
                homeAbbrev = h.optString("abbrev"),
                awayAbbrev = a.optString("abbrev"),
                homeName =
                    h.optJSONObject("placeName")
                        ?.optString("default")
                        ?: h.optString("abbrev"),
                awayName =
                    a.optJSONObject("placeName")
                        ?.optString("default")
                        ?: a.optString("abbrev"),
                gameState = g.optString("gameState")
            )
        }

        return out
    }

    fun currentSeasonGames(
        team: String,
        seasonId: String
    ): List<JSONObject> {

        val root = JSONObject(
            Net.get(
                "https://api-web.nhle.com/v1/club-schedule-season/$team/$seasonId"
            )
        )

        val games = root.optJSONArray("games") ?: return emptyList()

        return (0 until games.length()).map {
            games.getJSONObject(it)
        }
    }
}
