# Prompt: AppFunctions Integration (Android System Integration)

## Priorität: 🟢 NIEDRIG–MITTEL (Zukunftsinvestition)
## Empfohlenes Modell: **Claude Sonnet 4.6 (Thinking)**
> Begründung: Neues, noch wenig dokumentiertes API. Erfordert tiefes Verständnis der App-Domäne (BirthdayBuddy-Workflows), um sinnvolle AppFunctions zu identifizieren und korrekt zu implementieren. Reasoning-intensive Aufgabe.

---

## Kontext

Android AppFunctions ermöglichen es, App-Workflows dem Android-System, Google Assistant, und KI-Agenten (on-device) bereitzustellen – ohne die App-UI öffnen zu müssen.

**Verwende den `appfunctions`-Skill** (`.agents/skills/appfunctions/SKILL.md`) vollständig als Leitfaden.

---

## Aufgabe

### Schritt 1: AppFunctions-Skill lesen
Lese `.agents/skills/appfunctions/SKILL.md` vollständig durch, bevor du irgendwelchen Code schreibst.

### Schritt 2: Relevante Workflows in BirthdayBuddy identifizieren

Analysiere die App und identifiziere Workflows, die als AppFunctions sinnvoll sind:

**Wahrscheinlich geeignete Workflows:**
- `GetUpcomingBirthdays` – Liste der nächsten Geburtstage abrufen (nächste X Tage)
- `GetContactBirthday` – Geburtstag einer bestimmten Kontaktperson abrufen (by Name)
- `SendBirthdayMessage` – Direktlink zum Senden einer Geburtstagsnachricht via einer Messaging-App (WhatsApp, Signal etc.) öffnen
- `AddBirthdayToContact` – Geburtstag eines Kontakts hinzufügen/bearbeiten

### Schritt 3: AppFunctions implementieren
- Erstelle `domain/appfunctions/BirthdayAppFunctions.kt`
- Nutze `@AppFunctionSerializable` für Parameter und Return-Types
- Implementiere `AppFunctionManager`-Integration
- KDoc auf Englisch, präzise Beschreibung für KI-Agenten (welche Parameter, was die Funktion tut, Constraints)

### Schritt 4: Hilt-Integration
- AppFunction-Handler via Hilt injizieren
- Nutze bestehende Use Cases (`GetContactsUseCase` etc.) als Datenschicht

### Schritt 5: AndroidManifest.xml
- AppFunctions korrekt im Manifest deklarieren

### Schritt 6: Tests
- Unit-Tests für AppFunction-Handler in `src/test/java/.../domain/appfunctions/`

### Schritt 7: Dokumentation
- `PROJECT_STRUCTURE.md` aktualisieren mit neuem `domain/appfunctions/`-Modul
- CHANGELOG.md aktualisieren

---

## Wichtige Hinweise
- AppFunctions müssen klare, atomare Aktionen repräsentieren
- Parameter-Namen und KDoc-Beschreibungen sind kritisch für korrekte KI-Interpretation
- Teste mit dem AppFunction-Tester in Android Studio
