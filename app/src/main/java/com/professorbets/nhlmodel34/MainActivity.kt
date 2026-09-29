package com.professorbets.nhlmodel34

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NHLModelApp() }
    }
}

private val Gold=Color(0xFFFFD54A)
private val Green=Color(0xFF59E391)
private val Red=Color(0xFFFF7777)
private val Background=Color(0xFF080A0F)
private val CardBg=Color(0xFF121621)
private val Muted=Color(0xFFA6ADBB)

@Composable
fun NHLModelApp(vm:LiveViewModel = viewModel()) {
    MaterialTheme(colorScheme=darkColorScheme(primary=Gold,secondary=Green,background=Background,surface=CardBg,onBackground=Color.White,onSurface=Color.White)) {
        var tab by remember { mutableIntStateOf(0) }
        Scaffold(containerColor=Background,bottomBar={
            NavigationBar(containerColor=CardBg) {
                NavigationBarItem(selected=tab==0,onClick={tab=0},icon={Text("📅")},label={Text("Picks")})
                NavigationBarItem(selected=tab==1,onClick={tab=1},icon={Text("🏒")},label={Text("Matchup")})
                NavigationBarItem(selected=tab==2,onClick={tab=2},icon={Text("⚙️")},label={Text("Settings")})
            }
        }) { p ->
            Box(Modifier.padding(p)) {
                when(tab) {
                    0 -> LiveSlateScreen(vm)
                    1 -> ManualPredictorScreen()
                    else -> ModelInfoScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun LiveSlateScreen(vm:LiveViewModel) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { if(state.games.isEmpty() && !state.loading) vm.refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("PROFESSOR BETS",color=Gold,fontWeight=FontWeight.Black,fontSize=27.sp)
        Text("NHL Model 3.4 • Today's Picks",color=Muted)

        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            OutlinedButton(onClick={vm.refresh(state.date.minusDays(1))}){Text("‹")}
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Text(state.date.format(DateTimeFormatter.ofPattern("EEE MMM d")),fontWeight=FontWeight.Bold)
                if(state.date==LocalDate.now()) Text("TODAY",color=Green,fontSize=11.sp,fontWeight=FontWeight.Bold)
            }
            OutlinedButton(onClick={vm.refresh(state.date.plusDays(1))}){Text("›")}
        }
        Button(onClick={vm.refresh(state.date)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),enabled=!state.loading) {
            Text(if(state.loading) "UPDATING…" else "REFRESH PICKS",fontWeight=FontWeight.Black)
        }
        if(state.loading) LinearProgressIndicator(Modifier.fillMaxWidth(),color=Green)
        state.error?.let { Text("Refresh error: $it",color=Red) }
        if(!state.loading && state.games.isEmpty()) {
            SectionCard("Slate") { Text("No regular-season NHL games found for this date.",color=Muted) }
        }
        state.games.forEach { LiveGameCard(it) }
        Text("Live Elo is reconstructed from current-season NHL results and starts each season at 1500. The research V3.4 Elo carried across seasons, so live Elo is an approximation until the app is given a persistent multi-season seed.",color=Muted,fontSize=11.sp)
    }
}

@Composable
private fun LiveGameCard(lp:LivePrediction) {
    val g=lp.game; val p=lp.prediction
    SectionCard("${g.awayAbbrev} @ ${g.homeAbbrev}") {
        Text(g.startTimeUtc.replace("T"," ").replace("Z"," UTC"),color=Muted,fontSize=12.sp)
        if(p==null) {
            Text(lp.note ?: "Model inputs unavailable",color=Red)
            return@SectionCard
        }
        val fav=if(p.homeProbability>=.5) g.homeAbbrev else g.awayAbbrev
        val fp=maxOf(p.homeProbability,p.awayProbability)
        Text("PROFESSOR BETS PICK",color=Gold,fontWeight=FontWeight.Bold,fontSize=11.sp)
        Text("$fav • ${p.signal}",color=Green,fontWeight=FontWeight.Black,fontSize=24.sp)
        Stat(g.homeAbbrev,pct(p.homeProbability))
        LinearProgressIndicator(progress={p.homeProbability.toFloat()},modifier=Modifier.fillMaxWidth(),color=Green)
        Stat(g.awayAbbrev,pct(p.awayProbability))
        lp.market?.let { m ->
            HorizontalDivider(color=Color(0xFF303744))
            Text("${m.bookmaker} MONEYLINE",color=Gold,fontWeight=FontWeight.Bold,fontSize=11.sp)
            Stat(g.homeAbbrev,american(m.homeOdds))
            Stat(g.awayAbbrev,american(m.awayOdds))
            val fair=ModelEngine.noVig(m.homeOdds,m.awayOdds)
            val homeEdge=p.homeProbability-fair.first
            val awayEdge=p.awayProbability-fair.second
            val bestHomeEv=ModelEngine.expectedValue(p.homeProbability,m.homeOdds)
            val bestAwayEv=ModelEngine.expectedValue(p.awayProbability,m.awayOdds)
            val chooseHome=homeEdge>=awayEdge
            val edge=if(chooseHome)homeEdge else awayEdge
            val ev=if(chooseHome)bestHomeEv else bestAwayEv
            Text("Best model edge: ${if(chooseHome) g.homeAbbrev else g.awayAbbrev} ${signedPct(edge)}",color=if(edge>0)Green else Red,fontWeight=FontWeight.Bold)
            Text("EV per $1: ${signedPct(ev)}",color=if(ev>0)Green else Red)
        }
        lp.note?.let { Text(it,color=Muted,fontSize=11.sp) }
    }
}

@Composable
private fun ManualPredictorScreen() {
    var homeTeam by remember{mutableStateOf("HOME")}; var awayTeam by remember{mutableStateOf("AWAY")}
    var homeElo by remember{mutableStateOf("1500")}; var awayElo by remember{mutableStateOf("1500")}
    var homeB2B by remember{mutableStateOf(false)}; var awayB2B by remember{mutableStateOf(false)}
    var homeXg20 by remember{mutableStateOf("0.500")}; var awayXg20 by remember{mutableStateOf("0.500")}
    var homeCorsi20 by remember{mutableStateOf("0.500")}; var awayCorsi20 by remember{mutableStateOf("0.500")}
    var homeXg10 by remember{mutableStateOf("0.500")}; var awayXg10 by remember{mutableStateOf("0.500")}
    var homeGsax20 by remember{mutableStateOf("0.000")}; var awayGsax20 by remember{mutableStateOf("0.000")}
    var homeOdds by remember{mutableStateOf("")}; var awayOdds by remember{mutableStateOf("")}
    var prediction by remember{mutableStateOf<ModelEngine.Prediction?>(null)}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("MANUAL V3.4",color=Gold,fontWeight=FontWeight.Black,fontSize=27.sp)
        SectionCard("Matchup") { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextFieldSmall("Home",homeTeam,{homeTeam=it},Modifier.weight(1f),false);TextFieldSmall("Away",awayTeam,{awayTeam=it},Modifier.weight(1f),false)} }
        SectionCard("Team strength") {
            MetricPair("Pregame Elo",homeElo,{homeElo=it},awayElo,{awayElo=it})
            MetricPair("20-game 5v5 xG share",homeXg20,{homeXg20=it},awayXg20,{awayXg20=it})
            MetricPair("20-game 5v5 Corsi share",homeCorsi20,{homeCorsi20=it},awayCorsi20,{awayCorsi20=it})
            MetricPair("10-game 5v5 xG share",homeXg10,{homeXg10=it},awayXg10,{awayXg10=it})
            MetricPair("20-game team GSAx/60",homeGsax20,{homeGsax20=it},awayGsax20,{awayGsax20=it})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){CheckLine("Home B2B",homeB2B){homeB2B=it};CheckLine("Away B2B",awayB2B){awayB2B=it}}
        }
        SectionCard("Sportsbook odds • optional") { MetricPair("American moneyline",homeOdds,{homeOdds=it},awayOdds,{awayOdds=it}) }
        Button(onClick={prediction=ModelEngine.predict(ModelEngine.Inputs(num(homeElo,1500.0),num(awayElo,1500.0),homeB2B,awayB2B,num(homeXg20,.5),num(awayXg20,.5),num(homeCorsi20,.5),num(awayCorsi20,.5),num(homeXg10,.5),num(awayXg10,.5),num(homeGsax20,0.0),num(awayGsax20,0.0)))},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("RUN V3.4",fontWeight=FontWeight.Black)}
        prediction?.let{ PredictionCard(homeTeam,awayTeam,it,homeOdds,awayOdds) }
    }
}

@Composable
private fun PredictionCard(home:String,away:String,p:ModelEngine.Prediction,homeOdds:String,awayOdds:String) {
    SectionCard("Prediction") {
        val fav=if(p.homeProbability>=.5)home else away
        Text("$fav • ${p.signal}",color=Green,fontWeight=FontWeight.Black,fontSize=23.sp)
        Stat(home,pct(p.homeProbability)); LinearProgressIndicator(progress={p.homeProbability.toFloat()},modifier=Modifier.fillMaxWidth(),color=Green); Stat(away,pct(p.awayProbability))
        val ho=homeOdds.toDoubleOrNull(); val ao=awayOdds.toDoubleOrNull()
        if(ho!=null&&ao!=null&&ho!=0.0&&ao!=0.0) {
            val fair=ModelEngine.noVig(ho,ao); val hp=p.homeProbability-fair.first; val ap=p.awayProbability-fair.second
            Stat("Home edge",signedPct(hp)); Stat("Away edge",signedPct(ap))
        }
    }
}

@Composable
private fun ModelInfoScreen(vm:LiveViewModel) {
    var key by remember{mutableStateOf(vm.oddsKey())}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("MODEL & SETTINGS",color=Gold,fontWeight=FontWeight.Black,fontSize=27.sp)
        SectionCard("V3.4 backtest") { Stat("Games","7,614");Stat("Accuracy","59.71%");Stat("AUC","0.6282");Stat("Brier","0.235957");Stat("Log loss","0.664443") }
        SectionCard("Core features") { listOf("Pregame Elo","Home ice","Back-to-back differential","20-game 5v5 xG share","20-game 5v5 Corsi share","10-game 5v5 xG share","20-game team GSAx/60 proxy").forEach{Text("• $it")} }
        SectionCard("Live odds") {
            OutlinedTextField(value=key,onValueChange={key=it},label={Text("The Odds API key")},modifier=Modifier.fillMaxWidth(),singleLine=true)
            Button(onClick={vm.saveOddsKey(key)},modifier=Modifier.fillMaxWidth()){Text("SAVE ODDS KEY")}
            Text("Optional. Without a key, the app still loads schedule + V3.4 probabilities. With a key it also loads NHL moneylines and calculates no-vig edge and EV.",color=Muted,fontSize=11.sp)
        }
        SectionCard("Goalies") {
            Text("Current live build does not auto-adjust for a projected starter. The V3.4 research model has only validated the team GSAx proxy; the individual starting-goalie module is still awaiting historical starter validation.",color=Muted)
        }
        SectionCard("Live data sources") {
            Text("• NHL public schedule/results API")
            Text("• MoneyPuck team game-by-game CSVs")
            Text("• Optional The Odds API moneylines")
        }
    }
}

@Composable private fun SectionCard(title:String,content:@Composable ColumnScope.()->Unit){ Card(colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){Text(title.uppercase(),color=Gold,fontWeight=FontWeight.Bold,fontSize=12.sp);content()}} }
@Composable private fun MetricPair(label:String,h:String,onH:(String)->Unit,a:String,onA:(String)->Unit){Text(label,color=Muted,fontSize=12.sp);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){TextFieldSmall("Home",h,onH,Modifier.weight(1f),true);TextFieldSmall("Away",a,onA,Modifier.weight(1f),true)}}
@Composable private fun TextFieldSmall(label:String,value:String,onChange:(String)->Unit,modifier:Modifier,numeric:Boolean){OutlinedTextField(value=value,onValueChange=onChange,label={Text(label)},modifier=modifier,singleLine=true,keyboardOptions=if(numeric)KeyboardOptions(keyboardType=KeyboardType.Decimal)else KeyboardOptions.Default)}
@Composable private fun CheckLine(label:String,checked:Boolean,onChecked:(Boolean)->Unit){Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=checked,onCheckedChange=onChecked);Text(label)}}
@Composable private fun Stat(label:String,value:String){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(label,color=Muted);Text(value,fontWeight=FontWeight.Bold)}}
private fun num(s:String,fallback:Double)=s.toDoubleOrNull()?:fallback
private fun pct(x:Double)=String.format(Locale.US,"%.1f%%",x*100.0)
private fun signedPct(x:Double)=String.format(Locale.US,"%+.1f%%",x*100.0)
private fun american(x:Double)=if(x>0)"+${x.toInt()}" else x.toInt().toString()
