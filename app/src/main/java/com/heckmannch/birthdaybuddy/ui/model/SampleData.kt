package com.heckmannch.birthdaybuddy.ui.model

/**
 * Zentraler Ort für Testdaten, die in Previews und Tests verwendet werden.
 */
object SampleData {
    val giftIdea1 = GiftIdea(id = "g1", text = "Socken", isChecked = false)
    val giftIdea2 = GiftIdea(id = "g2", text = "Wein", isChecked = true)
    
    val contact1 = ContactUiModel(
        id = "1",
        contactId = "1",
        lookupKey = "k1",
        fullName = "Max Mustermann",
        dateText = "12. Mai",
        monthName = "Mai",
        imageUri = null,
        phoneNumber = "+49 123 456789",
        initials = "M",
        nextAge = 30,
        daysUntilNext = 5,
        isToday = false,
        hasWhatsApp = true,
        hasSignal = false,
        labels = listOf("Freunde"),
        giftIdeas = listOf(giftIdea1)
    )

    val contact2 = ContactUiModel(
        id = "2",
        contactId = "2",
        lookupKey = "k2",
        fullName = "Erika Mustermann",
        dateText = "Heute",
        monthName = "Mai",
        imageUri = null,
        phoneNumber = null,
        initials = "E",
        nextAge = 40,
        daysUntilNext = 0,
        isToday = true,
        hasWhatsApp = false,
        hasSignal = false,
        labels = listOf("Familie"),
        giftIdeas = emptyList()
    )

    val contact3 = ContactUiModel(
        id = "3",
        contactId = "3",
        lookupKey = "k3",
        fullName = "Lukas Kind",
        dateText = "In 2 Tagen",
        monthName = "Mai",
        imageUri = null,
        phoneNumber = null,
        initials = "L",
        nextAge = 5,
        daysUntilNext = 2,
        isToday = false,
        hasWhatsApp = false,
        hasSignal = false,
        labels = listOf("Familie"),
        giftIdeas = listOf(giftIdea1, giftIdea2)
    )

    val sampleContacts = listOf(contact1, contact2, contact3)

    val homeUiState = HomeUiState(
        contacts = sampleContacts,
        availableLabels = listOf("Familie", "Freunde", "Arbeit"),
    )
}
