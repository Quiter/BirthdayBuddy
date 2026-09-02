package com.heckmannch.birthdaybuddy.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AndroidPermissionCheckerTest {

    private val context: Context = mockk(relaxed = true)
    private lateinit var permissionChecker: AndroidPermissionChecker

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
        mockkStatic(NotificationManagerCompat::class)

        permissionChecker = AndroidPermissionChecker(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- Contacts Permission Tests ---

    @Test
    fun hasContactsPermission_whenPermissionGranted_returnsTrue() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val result = permissionChecker.hasContactsPermission()

        // Assert
        assertTrue(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        }
    }

    @Test
    fun hasContactsPermission_whenPermissionDenied_returnsFalse() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val result = permissionChecker.hasContactsPermission()

        // Assert
        assertFalse(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
        }
    }

    // --- Notification Permission Tests ---

    @Test
    fun hasNotificationPermission_whenNotificationsEnabled_returnsTrue() {
        // Arrange
        val notificationManagerCompat = mockk<NotificationManagerCompat>()
        every { NotificationManagerCompat.from(context) } returns notificationManagerCompat
        every { notificationManagerCompat.areNotificationsEnabled() } returns true

        // Act
        val result = permissionChecker.hasNotificationPermission()

        // Assert
        assertTrue(result)
        verify(exactly = 1) { NotificationManagerCompat.from(context) }
        verify(exactly = 1) { notificationManagerCompat.areNotificationsEnabled() }
    }

    @Test
    fun hasNotificationPermission_whenNotificationsDisabled_returnsFalse() {
        // Arrange
        val notificationManagerCompat = mockk<NotificationManagerCompat>()
        every { NotificationManagerCompat.from(context) } returns notificationManagerCompat
        every { notificationManagerCompat.areNotificationsEnabled() } returns false

        // Act
        val result = permissionChecker.hasNotificationPermission()

        // Assert
        assertFalse(result)
        verify(exactly = 1) { NotificationManagerCompat.from(context) }
        verify(exactly = 1) { notificationManagerCompat.areNotificationsEnabled() }
    }

    // --- Calendar Permission Tests ---

    @Test
    fun hasCalendarPermission_whenBothReadAndWriteGranted_returnsTrue() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val result = permissionChecker.hasCalendarPermission()

        // Assert
        assertTrue(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        }
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        }
    }

    @Test
    fun hasCalendarPermission_whenOnlyReadGranted_returnsFalse() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val result = permissionChecker.hasCalendarPermission()

        // Assert
        assertFalse(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        }
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        }
    }

    @Test
    fun hasCalendarPermission_whenOnlyWriteGranted_returnsFalse() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_GRANTED

        // Act
        val result = permissionChecker.hasCalendarPermission()

        // Assert
        assertFalse(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        }
        verify(exactly = 0) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        }
    }

    @Test
    fun hasCalendarPermission_whenBothDenied_returnsFalse() {
        // Arrange
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)
        } returns PackageManager.PERMISSION_DENIED

        // Act
        val result = permissionChecker.hasCalendarPermission()

        // Assert
        assertFalse(result)
        verify(exactly = 1) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR)
        }
    }
}
