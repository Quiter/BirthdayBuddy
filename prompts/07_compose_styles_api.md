# Prompt: Compose Styles API – Design System Modernisierung

## Priorität: 🟢 NIEDRIG (Innovation / Zukunftsinvestition)
## Empfohlenes Modell: **Claude Sonnet 4.6 (Thinking)**
> Begründung: Neues experimentelles API, das tiefes Verständnis des bestehenden Design-Systems (Dimensions.kt, Theme.kt) und der Custom-Komponenten erfordert. Komplex aber hohes Payoff bei Wartbarkeit.

---

## Kontext

Jetpack Compose hat die **Styles API** eingeführt (`androidx.compose.ui.styles`), die ermöglicht, UI-Properties (Padding, Farben, Formen) deklarativ als Styles zu bündeln und auf Komponenten anzuwenden – ähnlich wie CSS-Klassen.

BirthdayBuddy hat bereits ein solides Design-System via `Dimensions.kt` und `Theme.kt`. Die Styles API kann dieses weiter formalisieren.

**Verwende den `styles`-Skill** (`.agents/skills/styles/SKILL.md`) vollständig als Leitfaden.

---

## Aufgabe

### Schritt 1: Styles-Skill lesen
Lese `.agents/skills/styles/SKILL.md` vollständig, bevor du beginnst.

### Schritt 2: Abhängigkeiten prüfen
- Stelle sicher, dass die Compose BOM `2026.06.01` (aktuell genutzt) die Styles API enthält
- Prüfe ob zusätzliche Abhängigkeiten nötig sind

### Schritt 3: Pilotimplementierung – ContactListItem
Wähle `ContactListItem` (häufig verwendete, komplexe Komponente) als Pilot:
- Identifiziere alle hartcodierten Style-Parameter (Padding, Shape, Farbe, Elevation)
- Erstelle einen `ContactListItemStyle` mit `@Immutable`-Annotierung
- Implementiere `Modifier.styleable(style)` für Interaction-States (Pressed, Hovered)
- Stelle sicher, dass Default-Style Werte aus `Dimensions.kt` und `MaterialTheme` nutzt

### Schritt 4: Theme-Integration
- Registriere Custom-Styles in `BirthdayBuddyTheme { }` via `CompositionLocalProvider`
- Stelle sicher, dass Dark/Light Mode und AMOLED-Modus korrekt auf Styles reagieren

### Schritt 5: Weitere Kandidaten (nach Pilotvalidierung)
Priorisiere nach Nutzungshäufigkeit:
1. `NotificationRuleCard`
2. `LabelChip`  
3. `SettingsListItem`
4. `OnboardingCard`

### Schritt 6: Migration bestehender Komponenten
- Ersetze hardcodierte Werte in bestehenden Komponenten schrittweise durch Style-Attribute
- Halte `Dimensions.kt` als Single-Source-of-Truth für numerische Werte

### Schritt 7: Dokumentation & Tests
- KDoc für alle Style-Klassen (Englisch)
- Unit-Tests: Verifiziere dass Styles korrekt angewendet werden
- `PROJECT_STRUCTURE.md` um `ui/styles/`-Verzeichnis erweitern
- CHANGELOG.md aktualisieren

---

## Wichtige Hinweise
- Die Styles API ist möglicherweise noch `@ExperimentalComposeUiApi` – prüfe ob Suppression akzeptabel ist oder ob warten auf Stable sinnvoller ist
- Keine Breaking Changes an bestehenden Komponenten-Signaturen – Styles als optionale Parameter hinzufügen
