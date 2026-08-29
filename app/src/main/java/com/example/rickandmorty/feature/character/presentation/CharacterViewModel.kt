package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val characterUseCase: GetCharacterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    /** Guards against a retry/refresh stacking a second request on top of a running one. */
    private var loadJob: Job? = null

    init {
        onEvent(CharacterUiEvent.LoadCharacters)
    }

    /** The single entry point for the UI. */
    fun onEvent(event: CharacterUiEvent) {
        when (event) {
            CharacterUiEvent.LoadCharacters,
            CharacterUiEvent.Retry -> loadCharacters(isRefresh = false)

            CharacterUiEvent.Refresh -> loadCharacters(isRefresh = true)

            is CharacterUiEvent.CharacterClicked -> _uiState.update {
                it.copy(selectedCharacterId = event.characterId)
            }

            CharacterUiEvent.SelectionCleared -> _uiState.update {
                it.copy(selectedCharacterId = null)
            }

            CharacterUiEvent.ErrorShown -> _uiState.update {
                it.copy(errorMessageRes = null)
            }
        }
    }

    private fun loadCharacters(isRefresh: Boolean) {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    // A refresh keeps the current list on screen; a first load/retry does not.
                    isLoading = !isRefresh,
                    isRefreshing = isRefresh,
                    errorMessageRes = null
                )
            }

            // No Dispatchers.IO here: Retrofit's suspend functions already move the
            // request off the main thread.
            when (val result = characterUseCase()) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        characters = result.data.characters,
                        errorMessageRes = null
                    )
                }

                is Resource.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessageRes = result.error.toMessageRes()
                    )
                }
            }
        }
    }
}
