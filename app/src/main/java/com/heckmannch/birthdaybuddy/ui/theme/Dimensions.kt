package com.heckmannch.birthdaybuddy.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Standardized spacing and padding tokens.
 */
val SpacingNone = 0.dp
val SpacingExtraSmall = 4.dp
val SpacingSmall = 8.dp
val SpacingMedium = 12.dp
val SpacingNormal = 16.dp
val SpacingLarge = 24.dp
val SpacingExtraLarge = 32.dp

/**
 * Corner radius tokens for Material 3 shapes.
 */
val CornerRadiusExtraSmall = 4.dp
val CornerRadiusSmall = 8.dp
val CornerRadiusMedium = 12.dp
val CornerRadiusLarge = 16.dp
val CornerRadiusLargeIncreased = 20.dp
val CornerRadiusExtraLarge = 28.dp

/**
 * Standardized icon sizes.
 */
val IconSizeExtraSmall = 16.dp // Extra small icons (e.g., small edit indicators or status icons)
val IconSizeSmall = 18.dp
val IconSizeMedium = 20.dp // Medium icons (e.g., calendar picker buttons)
val IconSizeNormal = 24.dp
val IconSizeLarge = 32.dp
val IconSizeExtraLarge = 48.dp

/**
 * Common component heights and sizes.
 */
val SearchBarHeight = 56.dp
val OnboardingIllustrationHeight = 160.dp
val OnboardingIllustrationHeightSmall = 120.dp
val ContactImageSizeSmall = 40.dp
val ContactImageSizeNormal = 64.dp
val ContactImageSizeLarge = 96.dp
val ContactImageSizeOnboarding = 180.dp
val BirthdayQuoteIconContainerSize = 120.dp
val GridColumnMinWidth = 340.dp // Minimum column width for adaptive settings grids
val EmptyStatePadding = 64.dp  // Padding for centered empty state containers
val ElevationDefault = 2.dp    // Default tonal elevation for surface cards
val ElevationLow = 3.dp        // Low elevation for footers/cards
val ElevationHigh = 6.dp       // High elevation for picker dialogs/floating containers
val SidebarWidthExpanded = 240.dp
val SidebarWidthCollapsed = 80.dp
val SidebarHeaderSpacerHeight = 36.dp
val SidebarItemWidthCollapsed = 64.dp
val ContactItemSkeletonHeight = 72.dp
val ScrollbarTouchTargetWidth = 150.dp
val ScrollbarThumbWidth = 26.dp
val ScrollbarThumbHeight = 44.dp
val ScrollbarThumbElevation = 4.dp
val ScrollbarBubbleElevation = 8.dp
val ScrollbarArrowSize = 14.dp
val ScrollbarEstimatedHeaderHeight = 56.dp
val ScrollbarEstimatedContactHeight = 80.dp
val WheelPickerItemHeight = 48.dp

/**
 * Semantic Alpha (Transparency) levels.
 */
const val AlphaEmphasisHigh = 1.0f
const val AlphaEmphasisMedium = 0.7f
const val AlphaEmphasisLow = 0.5f
const val AlphaEmphasisSubtle = 0.3f
const val AlphaEmphasisDisabled = 0.38f
const val AlphaScrollbarThumb = 0.9f
const val AlphaContainerSubtle = 0.15f
const val AlphaContainerLow = 0.1f
const val AlphaContainerMuted = 0.08f
const val AlphaContainerMedium = 0.3f // Medium container alpha (e.g., for info cards)
const val AlphaSurfaceContainerLow = 0.5f
const val AlphaSurfaceContainerHigh = 0.6f
const val AlphaBorderSubtle = 0.2f
val ColorPickerComparisonSize = 56.dp
val ColorPickerSwatchMaxSize = 48.dp
val ColorPickerPanelHeight = 200.dp
val ColorPickerPanelLandscapeHeight = 150.dp
val ColorPickerPanelDefaultHeight = 200.dp
val ColorPickerDialogLandscapeMaxWidth = 600.dp
val ColorPickerMarkerRadius = 10.dp
val ColorPickerMarkerStroke = 3.dp
val ColorPickerCheckerCellSize = 6.dp
const val AlphaColorPickerMarkerShadow = 0.35f
const val AlphaColorPickerSwatchDisabled = 0.5f
const val ColorPickerSelectedBorderDarkenFactor = 0.4f
const val AnimDurationShort = 300   // Short animation duration in ms
const val AnimDurationMedium = 500  // Medium animation duration in ms
const val AnimDelayPermission = 300 // Delay before requesting permissions after navigation (ms)
const val ScrollbarFadeDurationMs = 300
const val ScrollbarBubbleDelayMs = 500L
const val ScrollbarFadeOutDelayMs = 1500L

/**
 * Onboarding Layout & Illustration Specific tokens.
 */
val OnboardingIllustrationCircleSize = 100.dp
val OnboardingFooterButtonWidth = 80.dp
val OnboardingCardWelcomeWidth = 200.dp
val OnboardingCardWelcomeHeight = 88.dp
val OnboardingCardNotifWidth = 220.dp
val OnboardingCardCalendarWidth = 160.dp
val OnboardingCardCalendarHeight = 132.dp
val OnboardingCardGuideWidth = 180.dp
val OnboardingCardGuideHeight = 130.dp
val OnboardingCardBorderWidth = 1.5.dp
val OnboardingCalendarHeaderHeight = 28.dp
val OnboardingCalendarCellSize = 20.dp
val OnboardingCalendarIndicatorSize = 6.dp
val OnboardingCalendarEventHeight = 26.dp
val OnboardingCalendarEventCornerRadius = 6.dp
val OnboardingCalendarEventPadding = 6.dp
val OnboardingCalendarEventIconSize = 12.dp
val OnboardingGuideCircleSize = 32.dp
val OnboardingGuideIconSize = 16.dp
val OnboardingGuideCheckIconSize = 10.dp
val OnboardingGuideStepCheckSize = 14.dp
val OnboardingGuideTextSpacer = 10.dp
val OnboardingGuideSmallCornerRadius = 3.dp
val OnboardingGuideTinySpacer = 2.dp
val ElevationOnboardingCard = 6.dp
val SelectedBorderWidth = 1.5.dp
val SelectedBorderWidthThick = 3.dp // Thick selected border (e.g., accent color picker)
val BirthdayBorderWidth = 2.dp
val SearchBarBorderWidth = 2.dp // Border width of the SearchBar when focused
val SearchBarFocusedElevation = 4.dp // Elevation of the SearchBar when focused
val BorderWidthThin = 1.dp
val BorderWidthThick = 2.dp
val ChipPaddingHorizontal = 6.dp // Horizontal padding inside chips
val ChipPaddingVertical = 2.dp   // Vertical padding inside chips
val CardCornerRadiusSmall = 4.dp
val CardCornerRadiusNormal = 16.dp
val CardCornerRadiusLarge = 24.dp
val WidgetCornerRadius = 12.dp
val WidgetItemMinHeight = 58.dp
val SpacingTiny = 2.dp
val MaxWidthMedium = 600.dp // Max width for medium screens
val MaxWidthExpanded = 840.dp // Max width for expanded screens
val IllustrationPreviewSize = 200.dp // Container size for previewing illustrations
val IllustrationCardContainerSize = 140.dp
val ContactAvatarHeaderSize = 68.dp

const val AlphaOnboardingCard = 0.9f
const val AlphaOnboardingCalendarDisabled = 0.4f
const val AlphaOnboardingCalendarInactive = 0.05f
const val OnboardingCalendarEventAlpha = 0.8f
const val AlphaWidgetCard = 0.8f

/**
 * Shared frosted glass card tokens used across Home, Labels, and Settings.
 */
val GlassCardHeightDefault = 80.dp
val GlassCardIconSize = 56.dp
val GlassCardMarkSize = 24.dp
val GlassCardSelectionMarkSize = 28.dp
val GlassCardTagIconSize = 12.dp
val GlassCardSubtitleIconSize = 16.dp
val GlassCardSwipeThreshold = 90.dp
val GlassCardSwipeIconSize = 22.dp
val GlassCardSkeletonTitleHeight = 18.dp
val GlassCardSkeletonSubtitleHeight = 14.dp

const val AlphaGlassCardFillStart = 0.70f
const val AlphaGlassCardFillMid = 0.58f
const val AlphaGlassCardFillEnd = 0.64f
const val AlphaGlassCardBorderHighlight = 0.65f
const val AlphaGlassCardBorderMid = 0.30f
const val AlphaGlassCardBorderEnd = 0.15f
const val AlphaGlassCardBorderSubtle = 0.20f
const val AlphaGlassCardFlatEdge = 0.30f
const val AlphaGlassCardSubtitle = 0.85f
const val AlphaGlassCardChipContainer = 0.20f
const val AlphaGlassCardMarkContainer = 0.35f
const val AlphaGlassCardShadow = 0.40f
const val AlphaGlassCardUnselected = 0.55f
const val AlphaGlassCardBorderNeutral = 0.40f
const val AlphaGlassCardBorderTinted = 0.25f

/**
 * Shared tokens for the Morphe-style expressive Settings UI system.
 */
val SettingsSegmentGap = 2.dp
val SettingsSegmentBlockGap = 16.dp
val SettingsContentMaxWidth = 560.dp
val SettingsHeaderControlSize = 40.dp
val SettingsOptionCardHeightCompact = 64.dp
val SettingsOptionIconSizeCompact = 22.dp
val SettingsSwatchSize = 48.dp
val SettingsEmptyStateIconSize = 56.dp
val SettingsAboutLogoSize = 72.dp
val SettingsEdgeFadeWidth = 24.dp
val SettingsNoticeActionMinHeight = 44.dp
val SettingsCardElevationLow = 1.dp

const val SettingsAnimDurationMs = 220
const val SettingsAnimDurationShortMs = 180
const val SettingsPressedCornerScale = 0.5f
const val SettingsPressedWidthRatio = 0.15f
const val SettingsMorphFadeFraction = 0.25f

const val AlphaSettingsHeaderControlActive = 0.45f
const val AlphaSettingsHeaderControlIdle = 0.18f
const val AlphaSettingsSelectionUnselectedDark = 0.35f
const val AlphaSettingsSelectionUnselectedLight = 0.60f
const val AlphaSettingsSelectionBorderDark = 0.55f
const val AlphaSettingsSelectionBorderLight = 0.45f
const val AlphaSettingsOutlineMuted = 0.50f
const val AlphaSettingsNoticeDivider = 0.20f
const val AlphaSettingsEmptyIcon = 0.50f
const val AlphaSettingsSubtitleMuted = 0.70f
const val AlphaSettingsInfoStatContainer = 0.30f
const val AlphaSettingsSwatchUnselected = 0.40f




