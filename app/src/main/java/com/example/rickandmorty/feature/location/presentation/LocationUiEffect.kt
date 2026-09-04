package com.example.rickandmorty.feature.location.presentation

/**
 * One-shot results of an event, kept out of state so they fire exactly once.
 */
sealed interface LocationUiEffect {

    data class NavigateToLocationDetail(val locationId: Int) : LocationUiEffect
}
