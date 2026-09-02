package com.heckmannch.birthdaybuddy.widget

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.heckmannch.birthdaybuddy.domain.model.Contact
import com.heckmannch.birthdaybuddy.ui.theme.SpacingExtraSmall
import com.heckmannch.birthdaybuddy.ui.theme.WidgetItemMinHeight
import org.junit.Test
import java.time.LocalDate

class WidgetLayoutHelperTest {

    private val defaultPadding = SpacingExtraSmall * 2 // 8.dp

    private fun createDummyContacts(count: Int): List<String> {
        return (1..count).map { "Contact $it" }
    }

    private fun createDummyDomainContact(id: Long, name: String): Contact {
        return Contact(
            localId = id,
            contactId = id.toString(),
            lookupKey = "key_$id",
            fullName = name,
            birthday = LocalDate.of(1990, 1, 1),
        )
    }

    // =========================================================================
    // 1. Standard Widget Sizes (e.g., 2x2, 4x2, 4x3, 4x4)
    // =========================================================================

    @Test
    fun `calculateLayout - 2x2 widget (130dp) - fits 2 items with evenly distributed height`() {
        // Arrange: 130dp total height - 8dp padding = 122dp available. 122 / 58 = 2 items fit.
        val totalHeight = 130.dp
        val contacts = createDummyContacts(5)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).hasSize(2)
        assertThat(result.displayContacts).containsExactly("Contact 1", "Contact 2").inOrder()
        // 122dp / 2 = 61dp
        assertThat(result.dynamicBlockHeight).isEqualTo(61.dp)
    }

    @Test
    fun `calculateLayout - 4x2 widget (110dp minimum) - fits 1 item taking full available height`() {
        // Arrange: 110dp total height - 8dp padding = 102dp available. 102 / 58 = 1 item fits.
        val totalHeight = 110.dp
        val contacts = createDummyContacts(5)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).hasSize(1)
        assertThat(result.displayContacts).containsExactly("Contact 1")
        // 102dp / 1 = 102dp
        assertThat(result.dynamicBlockHeight).isEqualTo(102.dp)
    }

    @Test
    fun `calculateLayout - 4x3 widget (190dp) - fits 3 items with evenly distributed height`() {
        // Arrange: 190dp - 8dp = 182dp available. 182 / 58 = 3 items fit.
        val totalHeight = 190.dp
        val contacts = createDummyContacts(6)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).hasSize(3)
        assertThat(result.displayContacts).containsExactly("Contact 1", "Contact 2", "Contact 3").inOrder()
        // 182dp / 3 = 60.666668dp
        assertThat(result.dynamicBlockHeight.value).isWithin(0.01f).of(60.67f)
    }

    @Test
    fun `calculateLayout - 4x4 widget (250dp) - fits 4 items with evenly distributed height`() {
        // Arrange: 250dp - 8dp = 242dp available. 242 / 58 = 4 items fit.
        val totalHeight = 250.dp
        val contacts = createDummyContacts(10)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).hasSize(4)
        assertThat(result.displayContacts).containsExactly("Contact 1", "Contact 2", "Contact 3", "Contact 4").inOrder()
        // 242dp / 4 = 60.5dp
        assertThat(result.dynamicBlockHeight).isEqualTo(60.5.dp)
    }

    // =========================================================================
    // 2. Extremely Small Heights (Below WidgetItemMinHeight)
    // =========================================================================

    @Test
    fun `calculateLayout - height smaller than WidgetItemMinHeight but larger than padding - clamps capacity to 1`() {
        // Arrange: 40dp total height - 8dp padding = 32dp available (< 58dp).
        val totalHeight = 40.dp
        val contacts = createDummyContacts(5)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: Minimum 1 item is taken, dynamic height is all remaining available height (32dp)
        assertThat(result.displayContacts).hasSize(1)
        assertThat(result.displayContacts).containsExactly("Contact 1")
        assertThat(result.dynamicBlockHeight).isEqualTo(32.dp)
    }

    @Test
    fun `calculateLayout - height smaller than vertical padding - available height is 0dp`() {
        // Arrange: 5dp total height - 8dp padding -> clamped to 0dp available.
        val totalHeight = 5.dp
        val contacts = createDummyContacts(3)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: 1 item taken, but height is 0dp
        assertThat(result.displayContacts).hasSize(1)
        assertThat(result.dynamicBlockHeight).isEqualTo(0.dp)
    }

    @Test
    fun `calculateLayout - zero total height with empty contacts - fallback to WidgetItemMinHeight`() {
        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = 0.dp,
            contacts = emptyList<String>(),
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).isEmpty()
        assertThat(result.dynamicBlockHeight).isEqualTo(WidgetItemMinHeight)
    }

    // =========================================================================
    // 3. Empty Contacts List vs. Many Contacts (> 10)
    // =========================================================================

    @Test
    fun `calculateLayout - empty contact list - returns empty display list and default min height`() {
        // Arrange: standard height, but no contacts available
        val totalHeight = 200.dp

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = emptyList<String>(),
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).isEmpty()
        assertThat(result.dynamicBlockHeight).isEqualTo(WidgetItemMinHeight)
    }

    @Test
    fun `calculateLayout - many contacts (greater than 10) on tall screen - capped at 10 items`() {
        // Arrange: Tall widget (800dp) -> 792dp available. 792 / 58 = 13 items capacity.
        val totalHeight = 800.dp
        val contacts = createDummyContacts(25)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: Max items is capped at 10
        assertThat(result.displayContacts).hasSize(10)
        assertThat(result.displayContacts).containsExactlyElementsIn(createDummyContacts(10)).inOrder()
        // 792dp / 10 = 79.2dp
        assertThat(result.dynamicBlockHeight).isEqualTo(79.2.dp)
    }

    @Test
    fun `calculateLayout - many contacts but smaller height - only fits capacity items`() {
        // Arrange: 130dp total height -> fits 2 items
        val totalHeight = 130.dp
        val contacts = createDummyContacts(20)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert
        assertThat(result.displayContacts).hasSize(2)
        assertThat(result.displayContacts).containsExactly("Contact 1", "Contact 2").inOrder()
        assertThat(result.dynamicBlockHeight).isEqualTo(61.dp)
    }

    // =========================================================================
    // 4. Exact Even Height Distribution
    // =========================================================================

    @Test
    fun `calculateLayout - fewer contacts than capacity - distributes full available height across available contacts`() {
        // Arrange: Available height = 188dp - 8dp = 180dp. Capacity is 180 / 58 = 3 items.
        // But only 2 contacts exist!
        val totalHeight = 188.dp
        val contacts = createDummyContacts(2)

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: 2 items displayed, each gets 180dp / 2 = 90dp
        assertThat(result.displayContacts).hasSize(2)
        assertThat(result.dynamicBlockHeight).isEqualTo(90.dp)
        assertThat(result.dynamicBlockHeight * result.displayContacts.size).isEqualTo(180.dp)
    }

    @Test
    fun `calculateLayout - single contact with large height - single contact occupies entire available height`() {
        // Arrange: Available height = 180dp. Only 1 contact exists.
        val totalHeight = 188.dp
        val contacts = listOf("Alice")

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: 1 item displayed, gets full 180dp
        assertThat(result.displayContacts).containsExactly("Alice")
        assertThat(result.dynamicBlockHeight).isEqualTo(180.dp)
    }

    @Test
    fun `calculateLayout - contacts count equals capacity - exact division`() {
        // Arrange: Available height = 180dp. 3 contacts exist.
        val totalHeight = 188.dp
        val contacts = listOf("Alice", "Bob", "Charlie")

        // Act
        val result = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        // Assert: 180dp / 3 = 60dp each
        assertThat(result.displayContacts).hasSize(3)
        assertThat(result.dynamicBlockHeight).isEqualTo(60.dp)
        assertThat(result.dynamicBlockHeight * 3).isEqualTo(180.dp)
    }

    // =========================================================================
    // 5. Overloads and Domain Contact Compatibility
    // =========================================================================

    @Test
    fun `calculateLayout - separate top and bottom padding overload - behaves identically`() {
        val totalHeight = 130.dp
        val contacts = createDummyContacts(5)

        val resultCombined = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            verticalPadding = 8.dp,
        )

        val resultSeparated = WidgetLayoutHelper.calculateLayout(
            totalHeight = totalHeight,
            contacts = contacts,
            topPadding = 4.dp,
            bottomPadding = 4.dp,
        )

        assertThat(resultSeparated.displayContacts).isEqualTo(resultCombined.displayContacts)
        assertThat(resultSeparated.dynamicBlockHeight).isEqualTo(resultCombined.dynamicBlockHeight)
    }

    @Test
    fun `calculateLayout - works seamlessly with domain Contact objects and destructuring`() {
        val contacts = listOf(
            createDummyDomainContact(1L, "Max Mustermann"),
            createDummyDomainContact(2L, "Erika Musterfrau"),
            createDummyDomainContact(3L, "Hans Dampf"),
        )

        val (displayContacts, dynamicBlockHeight) = WidgetLayoutHelper.calculateLayout(
            totalHeight = 130.dp,
            contacts = contacts,
            verticalPadding = defaultPadding,
        )

        assertThat(displayContacts).hasSize(2)
        assertThat(displayContacts[0].fullName).isEqualTo("Max Mustermann")
        assertThat(displayContacts[1].fullName).isEqualTo("Erika Musterfrau")
        assertThat(dynamicBlockHeight).isEqualTo(61.dp)
    }

    // =========================================================================
    // 6. Sub-calculation unit tests
    // =========================================================================

    @Test
    fun `calculateAvailableHeight - normal and edge cases`() {
        assertThat(WidgetLayoutHelper.calculateAvailableHeight(100.dp, 8.dp)).isEqualTo(92.dp)
        assertThat(WidgetLayoutHelper.calculateAvailableHeight(8.dp, 8.dp)).isEqualTo(0.dp)
        assertThat(WidgetLayoutHelper.calculateAvailableHeight(4.dp, 8.dp)).isEqualTo(0.dp)
    }

    @Test
    fun `calculateCapacity - minItemHeight non-positive - returns maxItems`() {
        assertThat(WidgetLayoutHelper.calculateCapacity(100.dp, minItemHeight = 0.dp)).isEqualTo(10)
    }
}
