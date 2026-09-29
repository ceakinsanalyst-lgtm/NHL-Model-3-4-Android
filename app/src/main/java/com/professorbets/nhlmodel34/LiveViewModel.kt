package com.professorbets.nhlmodel34

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class LiveViewModel(app:Application):AndroidViewModel(app) {
    data class UiState(val loading:Boolean=false,val date:LocalDate=LocalDate.now(),val games:List<LivePrediction> = emptyList(),val error:String?=null)
    private val _state=MutableStateFlow(UiState())
    val state:StateFlow<UiState> = _state

    private val prefs=app.getSharedPreferences("nhl_model_34",0)
    fun oddsKey():String=prefs.getString("odds_api_key","") ?: ""
    fun saveOddsKey(v:String){ prefs.edit().putString("odds_api_key",v.trim()).apply() }

    fun refresh(date:LocalDate=_state.value.date) {
        _state.value=_state.value.copy(loading=true,error=null,date=date)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val games=LiveRepository.load(date,oddsKey())
                _state.value=UiState(false,date,games,null)
            } catch(e:Exception) {
                _state.value=UiState(false,date,emptyList(),e.message ?: e.javaClass.simpleName)
            }
        }
    }
}
