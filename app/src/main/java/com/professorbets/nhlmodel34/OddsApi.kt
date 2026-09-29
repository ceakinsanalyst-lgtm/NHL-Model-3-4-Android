package com.professorbets.nhlmodel34

import org.json.JSONArray
import java.net.URLEncoder

object OddsApi {
    private fun canonical(s:String)=s.lowercase().replace(".","").replace("-"," ").replace(Regex("\\s+")," ").trim()

    fun lines(apiKey:String): List<Triple<String,String,MarketLine>> {
        if (apiKey.isBlank()) return emptyList()
        val key=URLEncoder.encode(apiKey,"UTF-8")
        val url="https://api.the-odds-api.com/v4/sports/icehockey_nhl/odds?regions=us&markets=h2h&oddsFormat=american&apiKey=$key"
        val arr=JSONArray(Net.get(url))
        val out=mutableListOf<Triple<String,String,MarketLine>>()
        for(i in 0 until arr.length()) {
            val e=arr.getJSONObject(i)
            val home=e.optString("home_team"); val away=e.optString("away_team")
            val books=e.optJSONArray("bookmakers") ?: continue
            if(books.length()==0) continue
            val b=books.getJSONObject(0)
            val markets=b.optJSONArray("markets") ?: continue
            var homeOdds:Double?=null; var awayOdds:Double?=null
            for(m in 0 until markets.length()) {
                val market=markets.getJSONObject(m)
                if(market.optString("key")!="h2h") continue
                val outcomes=market.optJSONArray("outcomes") ?: continue
                for(o in 0 until outcomes.length()) {
                    val x=outcomes.getJSONObject(o); val name=x.optString("name"); val price=x.optDouble("price",Double.NaN)
                    if(canonical(name)==canonical(home)) homeOdds=price
                    if(canonical(name)==canonical(away)) awayOdds=price
                }
            }
            if(homeOdds!=null && awayOdds!=null) out += Triple(home,away,MarketLine(homeOdds!!,awayOdds!!,b.optString("title","Sportsbook")))
        }
        return out
    }
}
