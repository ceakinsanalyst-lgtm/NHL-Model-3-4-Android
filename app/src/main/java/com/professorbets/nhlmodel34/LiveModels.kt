package com.professorbets.nhlmodel34

data class NhlGame(
    val id: Long,
    val gameDate: String,
    val startTimeUtc: String,
    val homeAbbrev: String,
    val awayAbbrev: String,
    val homeName: String,
    val awayName: String,
    val gameState: String
)

data class TeamForm(
    val team: String,
    val elo: Double,
    val b2b: Boolean,
    val xg20: Double,
    val corsi20: Double,
    val xg10: Double,
    val gsax20: Double,
    val gamesUsed: Int
)

data class MarketLine(
    val homeOdds: Double,
    val awayOdds: Double,
    val bookmaker: String
)

data class LivePrediction(
    val game: NhlGame,
    val homeForm: TeamForm?,
    val awayForm: TeamForm?,
    val prediction: ModelEngine.Prediction?,
    val market: MarketLine? = null,
    val note: String? = null
)
