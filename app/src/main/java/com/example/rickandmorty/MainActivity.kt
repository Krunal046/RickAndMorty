package com.example.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.rickandmorty.core.theme.RickAndMortyTheme
import com.example.rickandmorty.feature.character.presentation.CharacterScreen
import com.example.rickandmorty.feature.character.presentation.CharacterViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyTheme {
                val viewModel: CharacterViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsState()
                val characters = viewModel.characters.collectAsLazyPagingItems()

                CharacterScreen(
                    uiState = uiState,
                    characters = characters,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}
