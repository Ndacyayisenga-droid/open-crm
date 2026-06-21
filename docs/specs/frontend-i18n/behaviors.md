# Behaviors: Frontend i18n (DE/EN)

## Spracherkennung

### Browser-Sprache Deutsch wird erkannt

- **Given** ein Nutzer besucht die App zum ersten Mal und seine Browser-Sprache ist `de` oder `de-DE`
- **When** die App geladen wird
- **Then** wird die UI auf Deutsch angezeigt
- **And** `<html lang="de">` ist gesetzt
- **And** `localStorage` enthält `language: "de"`

### Browser-Sprache Englisch wird erkannt

- **Given** ein Nutzer besucht die App zum ersten Mal und seine Browser-Sprache ist `en` oder `en-US`
- **When** die App geladen wird
- **Then** wird die UI auf Englisch angezeigt
- **And** `<html lang="en">` ist gesetzt

### Unbekannte Browser-Sprache fällt auf Englisch zurück

- **Given** ein Nutzer besucht die App zum ersten Mal und seine Browser-Sprache ist `fr`
- **When** die App geladen wird
- **Then** wird die UI auf Englisch angezeigt (Fallback)
- **And** `<html lang="en">` ist gesetzt

### Gespeicherte Sprache hat Vorrang vor Browser-Sprache

- **Given** die Browser-Sprache ist `de`
- **And** in `localStorage` ist `language: "en"` gespeichert
- **When** die App geladen wird
- **Then** wird die UI auf Englisch angezeigt
- **And** `<html lang="en">` ist gesetzt

## Sprachwechsel

### Wechsel von Englisch zu Deutsch

- **Given** die aktuelle Sprache ist Englisch
- **When** der Nutzer auf "DE" im Sprach-Toggle klickt
- **Then** wechseln alle UI-Texte sofort auf Deutsch
- **And** `<html lang="de">` wird gesetzt
- **And** `localStorage` wird auf `language: "de"` aktualisiert
- **And** "DE" wird visuell als aktiv hervorgehoben (grün, fett)

### Wechsel von Deutsch zu Englisch

- **Given** die aktuelle Sprache ist Deutsch
- **When** der Nutzer auf "EN" im Sprach-Toggle klickt
- **Then** wechseln alle UI-Texte sofort auf Englisch
- **And** `<html lang="en">` wird gesetzt
- **And** `localStorage` wird auf `language: "en"` aktualisiert
- **And** "EN" wird visuell als aktiv hervorgehoben (grün, fett)

### Klick auf bereits aktive Sprache hat keine Wirkung

- **Given** die aktuelle Sprache ist Deutsch
- **When** der Nutzer auf "DE" klickt
- **Then** ändert sich nichts

### Sprachwahl bleibt nach Seitenwechsel erhalten

- **Given** der Nutzer hat die Sprache auf Deutsch gewechselt
- **When** der Nutzer zu einer anderen Seite navigiert (z.B. von Firmen zu Server-Health)
- **Then** bleibt die UI auf Deutsch

### Sprachwahl überlebt Browser-Neustart

- **Given** der Nutzer hat die Sprache auf Deutsch gewechselt
- **When** der Nutzer den Browser schließt und die App erneut öffnet
- **Then** wird die UI auf Deutsch angezeigt

## Sprach-Toggle UI

### Toggle ist in der Desktop-Sidebar sichtbar

- **Given** die App wird auf einem Desktop-Bildschirm angezeigt
- **When** die Sidebar sichtbar ist
- **Then** ist der Sprach-Toggle "DE | EN" am unteren Rand der Sidebar sichtbar

### Toggle ist im Mobile-Menü sichtbar

- **Given** die App wird auf einem mobilen Bildschirm angezeigt
- **When** der Nutzer das Hamburger-Menü öffnet
- **Then** ist der Sprach-Toggle "DE | EN" im geöffneten Menü sichtbar

### Aktive Sprache ist visuell hervorgehoben

- **Given** die aktuelle Sprache ist Deutsch
- **When** der Sprach-Toggle angezeigt wird
- **Then** ist "DE" in Grün und fett dargestellt
- **And** "EN" ist in gedämpftem Weiß dargestellt

## Übersetzung der UI-Elemente

### Navigation wird übersetzt

- **Given** die Sprache ist Englisch
- **When** die Sidebar angezeigt wird
- **Then** zeigt das Navigationsmenü "Companies" statt "Firmen"

### Firmen-Liste wird übersetzt

- **Given** die Sprache ist Englisch
- **When** die Firmenliste angezeigt wird
- **Then** sind alle Labels auf Englisch (z.B. "New Company", "Show Archived", Spaltenüberschriften, Pagination, Sortierung)

### Firmen-Formular wird übersetzt

- **Given** die Sprache ist Englisch
- **When** das Formular zum Erstellen/Bearbeiten einer Firma angezeigt wird
- **Then** sind alle Labels und Platzhalter auf Englisch (z.B. "Company Name", "Save", "Cancel")

### Firmen-Detail wird übersetzt

- **Given** die Sprache ist Englisch
- **When** die Detailansicht einer Firma angezeigt wird
- **Then** sind alle Labels auf Englisch (z.B. "Company Details", "Edit", "Delete", "Street", "City")

### Lösch-Dialog wird übersetzt

- **Given** die Sprache ist Englisch
- **When** der Lösch-Bestätigungsdialog angezeigt wird
- **Then** sind Titel, Beschreibung und Buttons auf Englisch

### Fehlermeldungen werden übersetzt

- **Given** die Sprache ist Englisch
- **When** ein Fehler auftritt (z.B. Firma kann nicht gelöscht werden wegen zugeordneter Kontakte)
- **Then** wird die Fehlermeldung auf Englisch angezeigt

### Health-Status wird übersetzt

- **Given** die Sprache ist Englisch
- **When** die Server-Health-Seite angezeigt wird
- **Then** sind Titel und Statusmeldungen auf Englisch

### Leere Zustände werden übersetzt

- **Given** die Sprache ist Englisch
- **And** es gibt keine Firmen
- **When** die Firmenliste angezeigt wird
- **Then** wird die Meldung "No companies found. Create your first company." auf Englisch angezeigt

## Edge Cases

### localStorage ist nicht verfügbar

- **Given** localStorage ist blockiert (z.B. durch Browser-Einstellungen)
- **When** die App geladen wird
- **Then** wird die Browser-Sprache verwendet (Fallback Englisch)
- **And** die App funktioniert normal, nur ohne Persistenz

### Dynamische Texte mit Platzhaltern

- **Given** die Sprache ist Englisch
- **When** der Lösch-Dialog für die Firma "Acme Corp" angezeigt wird
- **Then** wird der Text "Do you really want to delete the company 'Acme Corp'?" angezeigt (Firmenname korrekt eingesetzt)

### Pagination-Text mit Platzhaltern

- **Given** die Sprache ist Englisch
- **And** die Firmenliste hat 3 Seiten und der Nutzer ist auf Seite 2
- **When** die Pagination angezeigt wird
- **Then** wird "Page 2 of 3" angezeigt
