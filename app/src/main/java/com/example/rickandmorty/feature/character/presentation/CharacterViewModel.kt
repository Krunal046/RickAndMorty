package com.example.rickandmorty.feature.character.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val characterUseCase: GetCharacterUseCase
): ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val characterList = characterUseCase.getCharacterList()
            Log.d("CharacterViewModel", "CharacterList: $characterList")
        }
    }

}




