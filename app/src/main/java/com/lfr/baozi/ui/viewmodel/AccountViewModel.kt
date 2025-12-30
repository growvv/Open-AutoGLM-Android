package com.lfr.baozi.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lfr.baozi.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountUiState(
    val nickname: String = PreferencesRepository.DEFAULT_NICKNAME,
    val avatarUri: String = "",
    val inviteCode: String = "",
    val isLoggedIn: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)

class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val defaultInviteCode = "123456"

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.nickname.collect { nickname ->
                _uiState.value = _uiState.value.copy(nickname = nickname)
            }
        }
        viewModelScope.launch {
            preferencesRepository.avatarUri.collect { avatarUri ->
                _uiState.value = _uiState.value.copy(avatarUri = avatarUri)
            }
        }
        viewModelScope.launch {
            preferencesRepository.inviteCode.collect { inviteCode ->
                _uiState.value = _uiState.value.copy(inviteCode = inviteCode)
            }
        }
        viewModelScope.launch {
            preferencesRepository.isLoggedIn.collect { isLoggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = isLoggedIn)
            }
        }
    }

    fun updateNickname(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }

    fun updateInviteCode(inviteCode: String) {
        _uiState.value = _uiState.value.copy(inviteCode = inviteCode)
    }

    fun updateAvatarUri(avatarUri: String) {
        _uiState.value = _uiState.value.copy(avatarUri = avatarUri)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun saveProfile() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                preferencesRepository.saveNickname(state.nickname)
                preferencesRepository.saveAvatarUri(state.avatarUri)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "保存失败：${e.message}")
            }
        }
    }

    fun saveProfile(nickname: String, avatarUri: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                preferencesRepository.saveNickname(nickname)
                preferencesRepository.saveAvatarUri(avatarUri)
                _uiState.value = _uiState.value.copy(isSaving = false)
                onSuccess?.invoke()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "保存失败：${e.message}")
            }
        }
    }

    fun login() {
        val state = _uiState.value
        val code = state.inviteCode.trim().ifBlank { defaultInviteCode }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                preferencesRepository.saveNickname(state.nickname)
                preferencesRepository.saveAvatarUri(state.avatarUri)
                preferencesRepository.saveInviteCode(code)
                preferencesRepository.setLoggedIn(true)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "登录失败：${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            try {
                preferencesRepository.setLoggedIn(false)
                _uiState.value = _uiState.value.copy(isSaving = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "退出登录失败：${e.message}")
            }
        }
    }
}
