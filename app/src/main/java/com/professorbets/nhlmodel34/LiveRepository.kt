package com.professorbets.nhlmodel34

import java.time.LocalDate

object LiveRepository {
    fun load(date: LocalDate, oddsApiKey: String = ""): List<LivePrediction> {
        val games=NhlApi.schedule(date.toString())
        if(games.isEmpty()) return emptyList()
        val teams=games.flatMap{listOf(it.homeAbbrev,it.awayAbbrev)}.toSet()
        val ratings=EloRepository.currentRatings(teams,date)
        val odds=runCatching{OddsApi.lines(oddsApiKey)}.getOrDefault(emptyList())

        fun marketFor(g:NhlGame):MarketLine? = odds.firstOrNull { (h,a,_) ->
            h.contains(g.homeName,ignoreCase=true) || h.contains(g.homeAbbrev,ignoreCase=true)
        }?.third

        return games.map { g ->
            try {
                val hb=EloRepository.isBackToBack(g.homeAbbrev,date)
                val ab=EloRepository.isBackToBack(g.awayAbbrev,date)
                val hf=MoneyPuckRepository.teamForm(g.homeAbbrev,date,ratings.getOrDefault(g.homeAbbrev,1500.0),hb)
                val af=MoneyPuckRepository.teamForm(g.awayAbbrev,date,ratings.getOrDefault(g.awayAbbrev,1500.0),ab)
                val p=ModelEngine.predict(ModelEngine.Inputs(
                    hf.elo,af.elo,hf.b2b,af.b2b,hf.xg20,af.xg20,hf.corsi20,af.corsi20,hf.xg10,af.xg10,hf.gsax20,af.gsax20
                ))
                LivePrediction(g,hf,af,p,marketFor(g),
                    if(hf.gamesUsed<8 || af.gamesUsed<8) "Limited current-form sample; rolling data may lean on prior season." else null)
            } catch(e:Exception) {
                LivePrediction(g,null,null,null,null,"Data error: ${e.message ?: "unknown"}")
            }
        }
    }
}
