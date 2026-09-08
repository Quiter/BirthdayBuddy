/**
 * Hilt- und Dependency-Injection-Qualifier für die BirthdayBuddy-Anwendung.
 *
 * Definiert benutzerdefinierte Qualifier-Annotationen zur eindeutigen Auflösung
 * verschiedener Coroutine-Dispatcher und Lifecycle-Scopes.
 */
package com.heckmannch.birthdaybuddy.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
