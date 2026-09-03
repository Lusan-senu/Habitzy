package com.htj.habitzy.ui.settings.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htj.habitzy.domain.model.Profile
import com.htj.habitzy.domain.repository.ProfileRepository
import com.htj.habitzy.domain.repository.SettingsRepository
import com.htj.habitzy.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountUiState(
    val profile: Profile? = null,
    val appLockEnabled: Boolean = false,
    val hasPin: Boolean = false,
)

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    profileRepository: ProfileRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountUiState> = combine(
        profileRepository.observeProfile(),
        settingsRepository.appLockEnabled,
        settingsRepository.appLockPin,
    ) { profile, lock, pin ->
        AccountUiState(profile = profile, appLockEnabled = lock, hasPin = pin != null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState(),
    )

    fun enableAppLock(pin: String) {
        viewModelScope.launch {
            settingsRepository.setAppLockPin(PinHasher.hash(pin))
            settingsRepository.setAppLockEnabled(true)
        }
    }

    fun verifyAndDisable(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val stored = settingsRepository.appLockPin.first()
            val ok = stored != null && stored == PinHasher.hash(pin)
            if (ok) {
                settingsRepository.setAppLockPin(null)
                settingsRepository.setAppLockEnabled(false)
            }
            onResult(ok)
        }
    }
}
