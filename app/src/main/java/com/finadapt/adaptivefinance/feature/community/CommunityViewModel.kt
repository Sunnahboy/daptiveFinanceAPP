
package com.finadapt.adaptivefinance.feature.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.finadapt.adaptivefinance.data.remote.LeaderboardEntry
import com.finadapt.adaptivefinance.data.repository.FinanceRepository
import com.finadapt.adaptivefinance.feature.dashboard.DashboardViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityViewModel(
    private val repository: FinanceRepository,
    private val dashboardViewModel: DashboardViewModel
) : ViewModel() {

    private val _leaderboardData = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboardData: StateFlow<List<LeaderboardEntry>> = _leaderboardData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _hallOfFameData = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val hallOfFameData: StateFlow<List<LeaderboardEntry>> = _hallOfFameData.asStateFlow()
    // ← ADD THIS: Exposes spendable coins to the UI
    private val _spendableCoins = MutableStateFlow(repository.getSpendableCoins())


    val currentAnonName: String = repository.getAnonymousName()

    init {
        observeLiveLeaderboard()
        fetchHallOfFame()
    }

    private fun observeLiveLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getLiveLeaderboardStream().collect { liveData ->

                    val fairData = liveData.filter { it.xp < 15000 }
                    val sortedData = fairData.sortedByDescending { it.xp }

                    _leaderboardData.value = sortedData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    private fun fetchHallOfFame() {
        viewModelScope.launch {
            try {
                val history = repository.getHallOfFame()
                _hallOfFameData.value = history
            } catch (e: Exception) {
                Log.e("CommunityVM", "Failed to load Hall of Fame: ${e.message}")
            }
        }
    }

    fun sendCheerToUser(targetUserId: String, targetAnonName: String) {
        viewModelScope.launch {
            try {
                val success = repository.deductCoins(10)
                if (success) {
                    repository.sendAnonymousCheer(targetUserId)
                    _spendableCoins.value = repository.getSpendableCoins()
                    dashboardViewModel.refreshCoins()
                    Log.d("CommunityVM", "Successfully spent 10 coins to cheer for $targetAnonName")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class CommunityViewModelFactory(
    private val repository: FinanceRepository,
    private val dashboardViewModel: DashboardViewModel
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CommunityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CommunityViewModel(repository, dashboardViewModel) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}