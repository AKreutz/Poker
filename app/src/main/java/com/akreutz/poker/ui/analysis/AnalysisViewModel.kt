package com.akreutz.poker.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akreutz.poker.data.model.SkillLuckAnalysis
import com.akreutz.poker.data.model.computeSkillLuckAnalysis
import com.akreutz.poker.data.model.playerSessionResultsFrom
import com.akreutz.poker.data.repository.PokerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** UI state for the Analysis tab: the skill/luck decomposition, or null while there's too little data. */
data class AnalysisUiState(
    val analysis: SkillLuckAnalysis?,
    val minSessionsPerPlayer: Int,
    val isLoading: Boolean,
)

class AnalysisViewModel(private val repository: PokerRepository) : ViewModel() {
    private val minSessionsPerPlayer = MutableStateFlow(SkillLuckAnalysis.DEFAULT_MIN_SESSIONS_PER_PLAYER)

    val uiState: StateFlow<AnalysisUiState> = combine(
        repository.observeSessionsWithEntries(),
        minSessionsPerPlayer,
    ) { sessions, minSessions ->
        val results = playerSessionResultsFrom(sessions)
        AnalysisUiState(
            analysis = computeSkillLuckAnalysis(results, minSessionsPerPlayer = minSessions),
            minSessionsPerPlayer = minSessions,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(
            analysis = null,
            minSessionsPerPlayer = SkillLuckAnalysis.DEFAULT_MIN_SESSIONS_PER_PLAYER,
            isLoading = true,
        ),
    )

    fun setMinSessionsPerPlayer(value: Int) {
        minSessionsPerPlayer.value = value.coerceAtLeast(1)
    }
}
