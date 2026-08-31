package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterPagingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    getCharacterPaging: GetCharacterPagingUseCase
) : ViewModel() {

    /**
     * cachedIn keeps the loaded pages across configuration changes and makes the flow
     * safe to collect more than once. Without it a rotation refetches from page 1.
     */
    val characters: Flow<PagingData<CharacterModel>> =
        getCharacterPaging().cachedIn(viewModelScope)

    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    /** The single entry point for the UI. */
    fun onEvent(event: CharacterUiEvent) {
        when (event) {
            is CharacterUiEvent.CharacterClicked -> _uiState.update {
                it.copy(selectedCharacterId = event.characterId)
            }

            CharacterUiEvent.SelectionCleared -> _uiState.update {
                it.copy(selectedCharacterId = null)
            }
        }
    }
}
