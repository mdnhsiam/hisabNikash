package com.hisabnikash.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hisabnikash.app.core.time.CurrentMonthClock

@Composable
internal fun rememberMonthClock(createClock: () -> CurrentMonthClock = { CurrentMonthClock() }): CurrentMonthClock {
    // The finance ViewModels retain this flow across Activity/composition recreation.
    val model: MonthClockViewModel = viewModel(factory = remember {
        viewModelFactory { initializer { MonthClockViewModel(createClock()) } }
    })
    return model.clock
}

private class MonthClockViewModel(val clock: CurrentMonthClock) : ViewModel()
