package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val characterUseCase: GetCharacterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CharacterUiState>(CharacterUiState.Loading)
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
    }

    fun loadCharacters() {
        viewModelScope.launch {
            _uiState.value = CharacterUiState.Loading

            // No Dispatchers.IO here: Retrofit's suspend functions already move the
            // request off the main thread.
            _uiState.value = when (val result = characterUseCase()) {
                is Resource.Success -> CharacterUiState.Success(result.data.characters)
                is Resource.Error -> CharacterUiState.Error(result.error.toMessageRes())
            }
        }
    }

}
