package com.heckmannch.birthdaybuddy.ui.screens

import androidx.lifecycle.ViewModel
import com.heckmannch.birthdaybuddy.data.FilterManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LabelViewModel @Inject constructor(
    val filterManager: FilterManager
) : ViewModel()
