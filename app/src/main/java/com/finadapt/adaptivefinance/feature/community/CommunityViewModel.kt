package com.finadapt.adaptivefinance.feature.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.data.remote.LeaderboardEntry
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class CommunityViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    private val _leaderboardData = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardData: StateFlow<List<LeaderboardEntry>> = _leaderboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Grab the user's secure anonymous name so the UI can highlight their row!
    val currentAnonName: String = repository.getAnonymousName()

    init {
        fetchLeaderboard()
    }

    fun fetchLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getTopLeaderboard()
                _leaderboardData.value = data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}


class CommunityViewModelFactory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}