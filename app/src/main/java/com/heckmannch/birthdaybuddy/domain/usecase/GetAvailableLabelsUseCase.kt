package com.heckmannch.birthdaybuddy.domain.usecase

import com.heckmannch.birthdaybuddy.data.local.ContactLabels
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.domain.model.LabelConfig
import dagger.Reusable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Reusable UseCase that calculates the list of available labels for the filter bar.
 *
 * This computes the visible labels (user-defined labels currently in use by any contact,
 * the "no birthday" pseudo-label, anniversary and name day categories) based on active contacts,
 * label configurations, and app settings.
 */
@Reusable
class GetAvailableLabelsUseCase @Inject constructor() {

    /**
     * Calculates the list of currently available labels.
     *
     * @param contacts The stream of all contacts.
     * @param configs The stream of all label configurations.
     * @param otherEventsEnabled Whether anniversaries and name days are enabled.
     * @param labelsEnabled Whether label filtering is enabled globally.
     * @return A flow emitting the list of available labels.
     */
    operator fun invoke(
        contacts: Flow<List<Contact>>,
        configs: Flow<List<LabelConfig>>,
        otherEventsEnabled: Flow<Boolean>,
        labelsEnabled: Flow<Boolean>
    ): Flow<List<String>> = combine(
        contacts,
        configs,
        otherEventsEnabled,
        labelsEnabled
    ) { contactsVal, configsVal, otherEventsEnabledVal, labelsEnabledVal ->
        if (!labelsEnabledVal) return@combine emptyList()
        val inUseLabels = contactsVal.asSequence().flatMap { it.labels }.toSet()
        val configMap = configsVal.associateBy { it.name }

        // Pseudo-Label "Ohne Datum" Konfiguration laden und Sichtbarkeit prüfen
        val pseudoConfig = configMap[ContactLabels.LABEL_NO_BIRTHDAY]
        val showPseudo = contactsVal.any { it.birthday == null } &&
                pseudoConfig?.isHiddenFromFilter != true &&
                pseudoConfig?.isIgnored != true

        // Prüfen, ob aktive, nicht-versteckte User-Labels vorhanden sind
        val hasActiveUserLabels = inUseLabels.any { name ->
            val config = configMap[name]
            config?.isSystem == false && !(config.isHiddenFromFilter) && !(config.isIgnored) && name != ContactLabels.LABEL_NO_BIRTHDAY
        }

        val showAnniversary = otherEventsEnabledVal && contactsVal.any { it.anniversary != null }
        val showNameDay = otherEventsEnabledVal && contactsVal.any { it.nameDay != null }

        // Wenn weder aktive User-Labels noch das Pseudo-Label noch andere Events aktiv sind -> Bar verstecken
        if (!hasActiveUserLabels && !showPseudo && !showAnniversary && !showNameDay) return@combine emptyList()

        val labels = mutableListOf<String>()

        // Zuerst die User-Label
        if (hasActiveUserLabels) {
            inUseLabels.asSequence()
                .filter { name ->
                    val config = configMap[name]
                    (config?.isSystem == false) && !(config.isHiddenFromFilter) && !(config.isIgnored) && name != ContactLabels.LABEL_NO_BIRTHDAY
                }
                .sorted()
                .forEach { labels.add(it) }
        }

        // "Ohne Datum" immer als Letztes von Geburtstagen, falls aktiv
        if (showPseudo) {
            labels.add(ContactLabels.LABEL_NO_BIRTHDAY)
        }

        // Weitere Ereignisse ganz rechts
        if (showAnniversary) {
            labels.add(ContactLabels.LABEL_ANNIVERSARY)
        }
        if (showNameDay) {
            labels.add(ContactLabels.LABEL_NAME_DAY)
        }

        labels
    }.flowOn(Dispatchers.Default)
}
