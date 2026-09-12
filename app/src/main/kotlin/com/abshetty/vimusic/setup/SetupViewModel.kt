package com.abshetty.vimusic.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abshetty.vimusic.core.data.auth.Account
import com.abshetty.vimusic.core.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {
    val account: StateFlow<Account?> = auth.account

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(activityContext: Context) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null

        viewModelScope.launch {
            auth.signIn(activityContext)
                .onFailure {
                    _error.value = it.message ?: "Sign-in did not complete"
                }
            _busy.value = false
        }
    }
}
