# Behaviors: Company Comments

## Kommentare anzeigen

### Kommentare werden beim Laden der Firmen-Detailseite angezeigt

- **Given** eine Firma hat 3 Kommentare
- **When** die Firmen-Detailseite geöffnet wird
- **Then** werden alle 3 Kommentare angezeigt
- **And** die Kommentare sind nach Erstellungsdatum absteigend sortiert (neueste zuerst)
- **And** jeder Kommentar zeigt Author, Datum und Text

### Leere Kommentarliste zeigt Hinweis

- **Given** eine Firma hat keine Kommentare
- **When** die Firmen-Detailseite geöffnet wird
- **Then** wird "Keine Kommentare vorhanden" angezeigt
- **And** das Eingabefeld für neue Kommentare ist trotzdem sichtbar

### Ladeindikator wird angezeigt

- **Given** die Kommentare werden vom Backend geladen
- **When** der Request noch nicht beantwortet ist
- **Then** wird ein Skeleton-Platzhalter angezeigt

### Datumsformat ist lesbar

- **Given** ein Kommentar wurde am 27.03.2026 um 15:30 Uhr erstellt
- **When** der Kommentar angezeigt wird
- **Then** wird das Datum in einem lesbaren Format angezeigt (z.B. "27.03.2026, 15:30")

## Kommentar erstellen

### Kommentar erfolgreich erstellen

- **Given** die Firmen-Detailseite ist geöffnet
- **When** der Nutzer einen Text in das Eingabefeld tippt und auf "Senden" klickt
- **Then** wird der Kommentar an das Backend gesendet
- **And** der neue Kommentar erscheint oben in der Liste
- **And** das Eingabefeld wird geleert

### Senden-Button ist deaktiviert bei leerem Text

- **Given** das Kommentar-Eingabefeld ist leer
- **When** der Nutzer den "Senden"-Button betrachtet
- **Then** ist der Button deaktiviert

### Senden-Button ist deaktiviert während des Sendens

- **Given** der Nutzer hat einen Kommentar abgeschickt
- **When** der Request noch läuft
- **Then** ist der "Senden"-Button deaktiviert
- **And** der Button zeigt "Wird gesendet..."

### Author wird vom Backend gesetzt

- **Given** der Nutzer erstellt einen Kommentar
- **When** der Request an das Backend gesendet wird
- **Then** enthält der Request-Body nur das `text`-Feld, keinen Author
- **And** das Backend setzt den Author automatisch auf "UNKNOWN"

### Fehler beim Erstellen zeigt modalen Dialog

- **Given** der Nutzer versucht einen Kommentar zu erstellen
- **When** das Backend einen Fehler zurückgibt (z.B. 500, Netzwerkfehler)
- **Then** wird ein modaler Dialog mit einer Fehlermeldung angezeigt
- **And** der eingegebene Text bleibt im Eingabefeld erhalten
- **And** der "Senden"-Button wird wieder aktiviert

### Neuer Kommentar erscheint ohne Seite neu zu laden

- **Given** der Nutzer hat einen Kommentar erfolgreich erstellt
- **When** der Kommentar in der Liste erscheint
- **Then** wurde die Seite nicht neu geladen
- **And** der neue Kommentar ist an erster Stelle in der Liste

## Pagination

### Erste 20 Kommentare werden initial geladen

- **Given** eine Firma hat 25 Kommentare
- **When** die Firmen-Detailseite geöffnet wird
- **Then** werden die 20 neuesten Kommentare angezeigt
- **And** ein "Mehr laden"-Button ist sichtbar

### Mehr laden fügt Kommentare hinzu

- **Given** 20 Kommentare werden angezeigt und es gibt weitere
- **When** der Nutzer auf "Mehr laden" klickt
- **Then** werden die nächsten 20 Kommentare an die bestehende Liste angehängt
- **And** die bereits angezeigten Kommentare bleiben erhalten

### Mehr laden verschwindet wenn alle geladen

- **Given** alle Kommentare einer Firma sind geladen
- **When** die letzte Seite erreicht ist (`page.last === true`)
- **Then** wird der "Mehr laden"-Button nicht mehr angezeigt

### Kein Mehr-laden-Button bei weniger als 20 Kommentaren

- **Given** eine Firma hat 5 Kommentare
- **When** die Firmen-Detailseite geöffnet wird
- **Then** werden alle 5 Kommentare angezeigt
- **And** kein "Mehr laden"-Button ist sichtbar

### Neuer Kommentar nach Mehr-laden

- **Given** der Nutzer hat über "Mehr laden" ältere Kommentare nachgeladen
- **When** der Nutzer einen neuen Kommentar erstellt
- **Then** erscheint der neue Kommentar an erster Stelle
- **And** die bereits geladenen Kommentare bleiben in der Liste

## Edge Cases

### Nur Whitespace im Eingabefeld

- **Given** der Nutzer hat nur Leerzeichen in das Eingabefeld eingegeben
- **When** der Nutzer den "Senden"-Button betrachtet
- **Then** ist der Button deaktiviert (Whitespace-only zählt als leer)

### Sehr langer Kommentartext

- **Given** der Nutzer gibt einen sehr langen Text ein (mehrere Absätze)
- **When** der Kommentar erstellt und angezeigt wird
- **Then** wird der vollständige Text angezeigt
- **And** das Layout bricht nicht

### Firma nicht gefunden

- **Given** die Firma wurde zwischenzeitlich gelöscht
- **When** das Frontend versucht Kommentare zu laden
- **Then** wird der 404-Fehler behandelt
- **And** der Kommentarbereich zeigt keine Kommentare

## Backend-Änderung

### CommentCreateDto ohne Author

- **Given** ein Client sendet einen POST-Request an `/api/companies/{id}/comments`
- **When** der Request-Body `{ "text": "Ein Kommentar" }` enthält (ohne Author)
- **Then** wird der Kommentar mit Author `"UNKNOWN"` erstellt
- **And** der Response enthält `author: "UNKNOWN"`

### Leerer Text wird abgelehnt

- **Given** ein Client sendet einen POST-Request mit `{ "text": "" }`
- **When** das Backend den Request validiert
- **Then** wird ein 400 Bad Request zurückgegeben
