package com.example.rickandmorty.feature.character.domain.usecase

import androidx.paging.PagingData
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCharacterPagingUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    operator fun invoke(): Flow<PagingData<CharacterModel>> = characterRepository.characterPaging()
}
