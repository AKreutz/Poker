package com.akreutz.poker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * A [ViewModelProvider.Factory] that delegates to a [create] lambda, for ViewModels whose
 * constructor args are just plain values (e.g. a repository) rather than something that needs
 * [androidx.lifecycle.SavedStateHandle]-based plumbing.
 */
class SimpleViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}
