package com.heckmannch.birthdaybuddy.ui.screens.home.components.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.heckmannch.birthdaybuddy.R
import com.heckmannch.birthdaybuddy.ui.components.AppSwitch
import com.heckmannch.birthdaybuddy.ui.theme.SpacingLarge
import com.heckmannch.birthdaybuddy.ui.theme.SpacingNormal
import com.heckmannch.birthdaybuddy.ui.theme.AlphaOnboardingCalendarDisabled
import com.heckmannch.birthdaybuddy.ui.theme.SpacingSmall
import com.heckmannch.birthdaybuddy.util.NO_YEAR_MARKER
import com.heckmannch.birthdaybuddy.util.hasYear
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Ein modularer, maßgeschneiderter Premium-DatePickerDialog in Walzen-Optik (Wheel-Picker),
 * der die optionale Eingabe des Geburtsjahres ermöglicht.
 */
@Composable
fun BirthdayDatePickerDialog(
    initialDate: LocalDate?,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val initialLocalDate = initialDate ?: today

    var includeYear by remember { mutableStateOf(initialDate?.hasYear ?: false) }
    var selectedDay by remember { mutableIntStateOf(initialLocalDate.dayOfMonth) }
    var selectedMonth by remember { mutableIntStateOf(initialLocalDate.monthValue) }
    var selectedYear by remember { mutableIntStateOf(if (initialLocalDate.hasYear) initialLocalDate.year else today.year) }

    // Bestimme die maximale Anzahl an Tagen im Monat basierend auf Monat und Jahr (Schaltjahr-sicher)
    val maxDays = remember(selectedMonth, selectedYear, includeYear) {
        val yearVal = if (includeYear) selectedYear else NO_YEAR_MARKER
        val monthEnum = Month.of(selectedMonth)
        monthEnum.length(Year.isLeap(yearVal.toLong()))
    }

    // Falls der ausgewählte Tag die maximalen Tage übersteigt (z.B. Wechsel von Jan -> Feb), korrigieren
    LaunchedEffect(maxDays) {
        if (selectedDay > maxDays) {
            selectedDay = maxDays
        }
    }

    // Hole Locale beobachtbar über LocalConfiguration
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) { configuration.locales[0] }

    // Dynamischer, lokalisierter Titel der aktuellen Selektion (z.B. "2. Juni" / "2. Juni 1989")
    val formattedDateText =
        remember(selectedDay, selectedMonth, selectedYear, includeYear, locale) {
            val yearVal = if (includeYear) selectedYear else NO_YEAR_MARKER
            val monthEnum = Month.of(selectedMonth)
            val maxDaysForMonth = monthEnum.length(Year.isLeap(yearVal.toLong()))
            val safeDay = selectedDay.coerceIn(1, maxDaysForMonth)

            val date = LocalDate.of(yearVal, selectedMonth, safeDay)
            if (includeYear) {
                val formatter = if (locale.language == "de") {
                    DateTimeFormatter.ofPattern("d. MMMM yyyy", locale)
                } else {
                    DateTimeFormatter.ofPattern("MMMM d, yyyy", locale)
                }
                date.format(formatter)
            } else {
                val formatter = if (locale.language == "de") {
                    DateTimeFormatter.ofPattern("d. MMMM", locale)
                } else {
                    DateTimeFormatter.ofPattern("MMMM d", locale)
                }
                date.format(formatter)
            }
        }

    // Lokalisierte Monatsnamen cachen
    val monthItems = remember(locale) {
        (1..12).map { Month.of(it).getDisplayName(TextStyle.SHORT, locale) }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingNormal)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingLarge)
            ) {
                // Dynamischer Header
                Text(
                    text = formattedDateText,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = SpacingLarge)
                )

                // Walzen-Bereich (Tag, Monat, optional Jahr)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingLarge),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tag-Walze
                    WheelPicker(
                        items = (1..maxDays).map { "%02d".format(it) },
                        selectedIndex = (selectedDay - 1).coerceIn(0, maxDays - 1),
                        onIndexSelected = { selectedDay = it + 1 },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    // Monat-Walze
                    WheelPicker(
                        items = monthItems,
                        selectedIndex = selectedMonth - 1,
                        onIndexSelected = { selectedMonth = it + 1 },
                        modifier = Modifier.weight(1.2f)
                    )

                    if (includeYear) {
                        val years = remember { (1900..today.year).map { it.toString() } }
                        Spacer(modifier = Modifier.width(SpacingSmall))

                        // Jahr-Walze
                        WheelPicker(
                            items = years,
                            selectedIndex = (selectedYear - 1900).coerceIn(0, years.size - 1),
                            onIndexSelected = { selectedYear = 1900 + it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Switch "Inklusive Jahr"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingLarge),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.birthday_picker_include_year),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    AppSwitch(
                        checked = includeYear,
                        onCheckedChange = { includeYear = it }
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.gift_dialog_cancel))
                    }
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    TextButton(
                        onClick = {
                            val finalYear = if (includeYear) selectedYear else NO_YEAR_MARKER
                            val monthEnum = Month.of(selectedMonth)
                            val maxDaysForMonth = monthEnum.length(Year.isLeap(finalYear.toLong()))
                            val safeDay = selectedDay.coerceIn(1, maxDaysForMonth)

                            val finalDate = LocalDate.of(finalYear, selectedMonth, safeDay)
                            onDateSelected(finalDate)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.gift_dialog_save))
                    }
                }
            }
        }
    }
}

/**
 * Ein wiederverwendbarer Wheel-Picker, der butterweiches Scrollen mit mechanischer Haptik
 * und einer ansprechenden 3D-Zylinderoptik in Compose realisiert.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 3,
    itemHeight: Dp = 48.dp,
) {
    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    // Synchronisation von außen nach innen
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && selectedIndex != lazyListState.firstVisibleItemIndex) {
            lazyListState.scrollToItem(selectedIndex)
        }
    }

    // Synchronisation von innen nach außen
    val currentSelection by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(currentSelection) {
        if (currentSelection in items.indices && currentSelection != selectedIndex) {
            onIndexSelected(currentSelection)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Box(
        modifier = modifier.height(itemHeight * visibleItemsCount),
        contentAlignment = Alignment.Center
    ) {
        // Trennlinien über/unter dem ausgewählten Element in der Mitte
        val density = LocalDensity.current
        val offsetTranslationPx = remember(itemHeight, density) {
            with(density) { (itemHeight / 2).toPx() }
        }

        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = -offsetTranslationPx },
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            thickness = 1.dp
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationY = offsetTranslationPx },
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            thickness = 1.dp
        )

        LazyColumn(
            state = lazyListState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * ((visibleItemsCount - 1) / 2)),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == currentSelection
                // Visuelle Zylinder-Optik durch Größen- und Opazitätsverlauf
                val scale = if (isSelected) 1.15f else 0.85f
                val alpha = if (isSelected) 1f else AlphaOnboardingCalendarDisabled

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                    )
                }
            }
        }
    }
}
