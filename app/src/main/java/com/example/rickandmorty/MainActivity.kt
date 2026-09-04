package com.example.rickandmorty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rickandmorty.core.navigation.MainScreen
import com.example.rickandmorty.core.theme.RickAndMortyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app is single-Activity: this hosts the theme and the navigation graph, and knows
 * nothing about any individual screen.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RickAndMortyTheme {
                MainScreen()
            }
        }
    }
}
