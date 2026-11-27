package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.di.DefaultDispatcher
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Reusable UseCase that calculates the list of available labels for the filter bar.
 *
 * This computes the visible labels (user-defined labels currently in use by any contact,
 * the "no birthday" pseudo-label, and enabled non-birthday event categories discovered via
 * [GetDetectedOtherEventsUseCase]) based on active contacts, label configurations, and app settings.
 */
@Reusable
class GetAvailableLabelsUseCase @Inject constructor(
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) {

    /**
     * Calculates the list of currently available labels.
     *
     * @param contacts The stream of all contacts.
     * @param configs The stream of all label configurations.
     * @param otherEventsEnabled Whether "Other events" are enabled globally.
     * @param labelsEnabled Whether label filtering is enabled globally.
     * @param disabledOtherEventLabels Event keys individually disabled in "Other events" settings.
     * @return A flow emitting the list of available labels.
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        configs: Flow<List<LabelConfig>>,
        otherEventsEnabled: Flow<Boolean>,
        labelsEnabled: Flow<Boolean>,
        disabledOtherEventLabels: Flow<Set<String>> = flowOf(emptySet()),
    ): Flow<List<String>> = combine(
        contacts,
        configs,
        otherEventsEnabled,
        labelsEnabled,
        disabledOtherEventLabels,
    ) { contactsVal, configsVal, otherEventsEnabledVal, labelsEnabledVal, disabledEventsVal ->
        if (!labelsEnabledVal) return@combine emptyList()
        val inUseLabels = contactsVal.asSequence().flatMap { it.labels }.toSet()
        val configMap = configsVal.associateBy { it.name }

        // Pseudo-Label "Ohne Datum" Konfiguration laden und Sichtbarkeit prüfen
        val pseudoConfig = configMap[ContactLabels.LABEL_NO_BIRTHDAY]
        val showPseudo = contactsVal.any { it.birthday == null } &&
            pseudoConfig?.isHiddenFromFilter != true &&
            pseudoConfig?.isIgnored != true

        // Prüfen, ob aktive, nicht-versteckte User-Labels vorhanden sind
        val activeUserLabels = inUseLabels.asSequence()
            .filter { name ->
                val config = configMap[name]
                (config?.isSystem == false) &&
                    !(config.isHiddenFromFilter) &&
                    !(config.isIgnored) &&
                    name != ContactLabels.LABEL_NO_BIRTHDAY &&
                    !ContactLabels.isOtherEventLabel(name)
            }
            .sorted()
            .toList()

        // Aktivierte „Weitere Ereignisse“ aus derselben Datenquelle ermitteln
        val activeOtherEventLabels = if (otherEventsEnabledVal) {
            GetDetectedOtherEventsUseCase.detect(contactsVal, disabledEventsVal)
                .asSequence()
                .filter { it.isEnabled }
                .map { it.eventKey }
                .filter { key ->
                    val config = configMap[key]
                    config?.isHiddenFromFilter != true && config?.isIgnored != true
                }
                .toList()
        } else {
            emptyList()
        }

        // Wenn weder aktive User-Labels noch das Pseudo-Label noch andere Events aktiv sind -> Bar verstecken
        if (activeUserLabels.isEmpty() && !showPseudo && activeOtherEventLabels.isEmpty()) {
            return@combine emptyList()
        }

        buildList {
            // 1. Zuerst die User-Labels
            addAll(activeUserLabels)

            // 2. "Ohne Datum" immer als Letztes von Geburtstagen, falls aktiv
            if (showPseudo) {
                add(ContactLabels.LABEL_NO_BIRTHDAY)
            }

            // 3. Weitere Ereignisse ganz rechts in kanonischer Reihenfolge
            addAll(activeOtherEventLabels)
        }
    }.flowOn(defaultDispatcher)
}
