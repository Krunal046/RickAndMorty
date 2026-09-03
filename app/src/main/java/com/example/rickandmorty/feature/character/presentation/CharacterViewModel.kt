package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterPagingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
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

    /**
     * Buffered so an effect emitted before the UI starts collecting - during a
     * configuration change, say - is delivered rather than dropped.
     */
    private val _effects = Channel<CharacterUiEffect>(Channel.BUFFERED)
    val effects: Flow<CharacterUiEffect> = _effects.receiveAsFlow()

    /** The single entry point for the UI. */
    fun onEvent(event: CharacterUiEvent) {
        when (event) {
            is CharacterUiEvent.CharacterClicked ->
                _effects.trySend(CharacterUiEffect.NavigateToCharacterDetail(event.characterId))
        }
    }
}
