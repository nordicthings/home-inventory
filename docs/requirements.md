# Fachliche Anforderungen

## Projektziel

Private Hausinventar-Web-App für die Familie.

## Version 1

Version 1 läuft lokal auf Jens' Rechner als Web-App.

Enthalten:

- Gegenstände per Formular anlegen
- Einträge bearbeiten
- Einträge nach Bestätigung löschen
- Durchsuchbare Liste
- Filter nach Ort und Kategorie

Noch nicht enthalten:

- Anmeldung
- Fotos oder PDF-Rechnungen
- Auswertungen oder Gesamtwerte
- Genaue Position am Ort
- Mobiler oder externer Zugriff

## Felder

Jeder Gegenstand hat diese Felder:

- Name
- Kategorie
- Schätzwert
- Freie Notiz

Name und Kategorie sind Pflichtfelder.

Jeder Gegenstand hat eine interne eindeutige ID. IDs werden in der Domäne grundsätzlich als UUID modelliert.

Es darf nicht mehrere Gegenstände mit gleichem Namen geben. Die Eindeutigkeit von Gegenstandsnamen ist unabhängig von Groß- und Kleinschreibung. `Laptop`, `laptop` und `LAPTOP` gelten fachlich als gleicher Name.

Die Durchsetzung der eindeutigen normalisierten Gegenstandsnamen erfolgt später im Application-Ring.

Der Name eines Gegenstands kann nachträglich geändert werden.

Wenn ein Gegenstand gelöscht wird, werden auch alle Zuordnungen dieses Gegenstands entfernt.

Ein Gegenstand gilt als vorhanden, sobald er im Datenbestand auftaucht. Dafür ist keine Ortszuordnung erforderlich.

Es gibt keinen Lebenszyklus für Gegenstände. Gegenstände, die nicht mehr vorhanden sind, werden aus dem Datenbestand gelöscht oder ihre Mengen werden reduziert.

Abgänge werden nicht als Historie dokumentiert. Der Grund eines Abgangs ist fachlich irrelevant. Verkauf und Verlust werden nicht unterschieden.

Wenn es mehrere Exemplare eines Gegenstands gibt, wird über Zuordnungen festgelegt, wie viele Exemplare sich an welchem Ort befinden.

Mengen entstehen ausschließlich durch Zuordnung zu Orten. Pro Gegenstand und Ort gibt es höchstens eine Menge. Die Gesamtmenge eines Gegenstands wird dynamisch aus den Mengen aller Ortszuordnungen berechnet.

Ein Gegenstand kann ohne Ortszuordnung existieren, z. B. wenn er bereits erfasst wurde, die Zuordnung aber erst später erfolgt. Solange ein Gegenstand keine Ortszuordnung hat, beträgt seine Gesamtmenge 0.

Die Menge eines Gegenstands an einem Ort ist immer eine positive ganze Zahl. Wird das letzte Exemplar eines Gegenstands von einem Ort entfernt, wird die Ortszuordnung gelöscht.

Ortsmengen sind nur in Ausnahmefällen direkt änderbar.

Eine Umlagerung zwischen Orten wird als eigene fachliche Aktion behandelt, damit die Korrektur in beiden Ortszuordnungen als atomare Operation modelliert werden kann.

Bei einer Umlagerung werden Quellort und Zielort angegeben. Die Menge am Quellort wird dekrementiert, die Menge am Zielort wird inkrementiert.

Ein Kauf oder Geschenk erzeugt fachlich einen Zugang. Dabei wird eine Zuordnung zwischen Gegenstand und Bezugsquelle mit Menge, Kaufpreis oder Kaufdatum erfasst. Existiert eine fachlich gleiche Zuordnung bereits, wird ihre Menge implizit erhöht.

Zugang und Ort werden separat erfasst. Ein Zugang erhöht nicht automatisch eine Ortsmenge.

Ortsmengen und Zugangsmengen dürfen voneinander abweichen. Das ist insbesondere für die anfängliche Inventarisierung gewollt. Differenzen können später durch geeignete Reports analysiert werden.

Nur ein Abgang erzeugt eine direkte Reduktion der Ortsmenge.

Alle Gegenstände werden fachlich gleich behandelt. Es gibt keine Unterscheidung zwischen einzeln erfassten Gegenständen und Gruppen gleichartiger Exemplare.

Die UI ist deutschsprachig. Geldwerte führen eine Währung mit. Aktuell wird in der Anwendung immer EUR verwendet.

## Werte und Bezugsquellen

Jeder Gegenstand hat einen Schätzwert. Wenn der Schätzwert unbekannt ist, wird 0 verwendet.

Der direkt am Gegenstand gepflegte Schätzwert spiegelt eine fachliche Einschätzung wider. Er kann auch dann gepflegt werden, wenn konkrete Kaufpreise vorhanden sind.

Im Extremfall hat ein Gegenstand keine konkrete Wertangabe. In diesem Fall sind Kaufpreise und Schätzwert 0.

Ein Gegenstand kann eine oder mehrere Bezugsquellen haben.

Eine Bezugsquelle kann ein physischer Laden, ein bestimmter Webshop oder eine allgemeine Angabe wie Flohmarkt sein.

Bezugsquellen sind pflegbar. Sie können in der Anwendung angelegt, bearbeitet und gelöscht werden.

Jede Bezugsquelle hat eine interne eindeutige ID. IDs werden in der Domäne grundsätzlich als UUID modelliert.

Namen von Bezugsquellen sind eindeutig. Die Eindeutigkeit ist unabhängig von Groß- und Kleinschreibung. Die Durchsetzung dieser Invariante erfolgt später im Application-Ring.

Eine Bezugsquelle kann Details haben. Details sind eine optionale Freitextangabe für Adresse, URL oder sonstige formlose Informationen.

Bezugsquellen haben keine weiteren fachlichen Attribute.

Eine Bezugsquelle, der noch Gegenstände zugeordnet sind, kann nicht gelöscht werden.

An der Zuordnung zwischen Gegenstand und Bezugsquelle hängen Menge, Kaufpreis und Kaufdatum. Das Kaufdatum ist optional.

Der Kaufpreis ist nicht optional. Wenn der Kaufpreis unbekannt ist, wird 0 verwendet.

Eine Zuordnung zwischen Gegenstand und Bezugsquelle darf ohne Kaufdatum existieren.

Mehrere Zuordnungen zwischen demselben Gegenstand und derselben Bezugsquelle sind erlaubt, wenn sich Datum, Kaufpreis oder beides unterscheidet.

Fachlich identische Zuordnungen zwischen demselben Gegenstand und derselben Bezugsquelle sind nicht erlaubt. Wenn ein Gegenstand am gleichen Tag zum gleichen Preis bei derselben Bezugsquelle gekauft oder erhalten wurde, wird dies über die Menge an der bestehenden Zuordnung abgebildet.

Die Menge an der Zuordnung zwischen Gegenstand und Bezugsquelle ist immer eine positive ganze Zahl.

Mengen an Bezugsquellen sind unabhängig von Mengen an Orten. Eine Bezugsquellenmenge beschreibt, wie viele Exemplare über diese Bezugsquelle bezogen wurden. Eine Ortsmenge beschreibt, wie viele Exemplare aktuell an einem Ort vorhanden sind.

Konkrete Kaufpreise übersteuern den Schätzwert bei der Berechnung des Werts eines Gegenstands. Ein Kaufpreis von 0 gilt nicht als konkreter Kaufpreis, sondern als unbekannt.

Wenn mehrere konkrete Kaufpreise vorhanden sind, errechnet sich der Wert eines Gegenstands als mengengewichteter Durchschnitt der konkreten Kaufpreise.

Wenn keine konkreten Kaufpreise vorhanden sind, aber ein Schätzwert vorhanden ist, entspricht der Wert des Gegenstands dem Schätzwert. Ein Schätzwert von 0 gilt nicht als konkreter Schätzwert, sondern als unbekannt.

Wenn weder konkrete Kaufpreise noch ein konkreter Schätzwert vorhanden sind, ist kein Wert bestimmbar.

Der berechnete Wert eines Gegenstands ist ein durchschnittlicher Stückwert.

Der Gesamtwert eines Gegenstands ergibt sich aus der Gesamtmenge an Orten und dem durchschnittlichen Stückwert.

Schätzwerte sind eine Zusatzinformation und können neben konkreten Kaufpreisen existieren.

Kaufpreise und Schätzwerte beziehen sich immer auf ein Stück.

Geldwerte sind nicht leer. Wenn ein Geldwert unbekannt ist, wird 0 verwendet. Der Wert 0 bedeutet fachlich immer „unbekannt“ und nicht „kostenlos“.

Auch ein kostenlos erhaltener Gegenstand hat fachlich einen Wert, selbst wenn dieser Wert nicht bekannt ist.

## Start-Orte

- Küche
- Wohnzimmer
- Bad
- Keller
- Schlafzimmer
- Irenas Zimmer
- Jens' Zimmer
- Gästezimmer
- Ankleidezimmer

Weitere Orte außerhalb des Hauses sollen fachlich möglich sein, z. B. Büro.

Die genannten Orte sind Startwerte.

Orte sind pflegbar. Sie können in der Anwendung angelegt, bearbeitet und gelöscht werden.

Jeder Ort hat eine interne eindeutige ID. IDs werden in der Domäne grundsätzlich als UUID modelliert.

Ortsnamen sind eindeutig. Die Eindeutigkeit ist unabhängig von Groß- und Kleinschreibung. Die Durchsetzung dieser Invariante erfolgt später im Application-Ring.

Jeder Ort hat einen Ortstyp. Der Ortstyp unterscheidet zwischen intern und extern.

Ein Ort, an dem sich noch Gegenstände befinden, kann nicht gelöscht werden.

## Start-Kategorien

- Möbel
- Multimedia & Unterhaltung
- Computer & Peripherie
- Kameras & Zubehör
- Haushaltsgeräte
- Körper / Gesundheit / Sport
- Software
- Audio / Video
- Schmuck
- Dekoration
- Beleuchtung
- Bücher
- Küchenausstattung
- Outdoor
- Kleidung
- Spielzeug
- Werkzeug
- Sonstiges

Die genannten Kategorien sind Startwerte.

Kategorien sind fachliche Werte. Sie haben keine interne ID.

Kategorien sind pflegbar. Sie können in der Anwendung angelegt, bearbeitet und gelöscht werden.

Eine Änderung des Namens einer Kategorie erzeugt fachlich eine neue Kategorie.

Kategorienamen sind eindeutig.

Die Eindeutigkeit von Kategorienamen ist unabhängig von Groß- und Kleinschreibung. Die Durchsetzung dieser Invariante erfolgt im Application-Ring über den normalisierten Kategorienamen.

Kategorien klassifizieren Gegenstände hinsichtlich ihres Gebrauchs, z. B. Möbel, Unterhaltungselektronik oder Computer & Peripherie.

Eine Kategorie hat außer ihrem Namen keine weiteren fachlichen Eigenschaften.

Jeder Gegenstand hat genau eine Kategorie.

Eine Kategorie, der noch Gegenstände zugeordnet sind, kann nicht gelöscht werden.

`Sonstiges` ist eine Kategorie wie alle anderen auch.

## Startdaten

Initiale Orte und Kategorien werden über ein Liquibase- oder SQL-Skript angelegt.

## Gestaltung

Die Gestaltung ist schlicht, klar und funktional.

Orte, Kategorien und Bezugsquellen werden in der UI alphabetisch sortiert.

## Glossar

Domänenobjekte werden im Code englisch benannt. Die Anwendungsoberfläche bleibt deutschsprachig.

| Deutsch | Englisch im Code | Erläuterung |
| --- | --- | --- |
| Gegenstand | Item | Eine eindeutig benannte Sache im Inventar. Ein Gegenstand kann ein einzelnes Exemplar oder mehrere gleichartige Exemplare beschreiben. |
| ID | ID | Interne eindeutige ID eines Domänenobjekts. IDs werden in der Domäne grundsätzlich als UUID modelliert. |
| Gegenstands-ID | Item ID | Interne eindeutige ID eines Gegenstands. Sie dient der technischen und fachlichen Identifikation, unabhängig vom Namen. |
| Gegenstandsname | Item Name | Fachlich eindeutiger Name eines Gegenstands. Es darf keine zwei Gegenstände mit gleichem Namen geben. Die Eindeutigkeit ist unabhängig von Groß- und Kleinschreibung und wird später im Application-Ring über den normalisierten Namen durchgesetzt. |
| Kategorie | Category | Fachliche Einordnung eines Gegenstands hinsichtlich seines Gebrauchs, z. B. Möbel, Unterhaltungselektronik oder Computer & Peripherie. Jeder Gegenstand hat genau eine Kategorie. Kategorien sind fachliche Werte ohne interne ID. |
| Ort | Location | Ein Ort, an dem sich Exemplare eines Gegenstands befinden können. Orte können Räume im Haus oder Orte außerhalb des Hauses sein, z. B. Büro. |
| Orts-ID | Location ID | Interne eindeutige ID eines Orts. |
| Ortsname | Location Name | Fachlich eindeutiger Name eines Orts. Die Eindeutigkeit ist unabhängig von Groß- und Kleinschreibung und wird später im Application-Ring über den normalisierten Namen durchgesetzt. |
| Ortstyp | Location Type | Unterscheidung, ob ein Ort intern oder extern ist. |
| Interner Ort | Internal Location | Ort innerhalb des Hauses oder Haushalts. |
| Externer Ort | External Location | Ort außerhalb des Hauses oder Haushalts, z. B. Büro. |
| Ortszuordnung | Item Location | Zuordnung eines Gegenstands zu einem Ort mit der dort befindlichen Menge. Pro Gegenstand und Ort gibt es höchstens eine Ortszuordnung. |
| Umlagerung | Relocation | Fachliche Aktion, bei der Exemplare eines Gegenstands atomar von einem Quellort an einen Zielort umgebucht werden. |
| Menge | Quantity | Anzahl der Exemplare eines Gegenstands an einem bestimmten Ort. Die Menge ist immer eine positive ganze Zahl. Die Gesamtmenge eines Gegenstands wird aus allen Ortszuordnungen berechnet. |
| Gesamtmenge | Total Quantity | Dynamisch berechnete Summe aller Mengen eines Gegenstands über alle Orte hinweg. |
| Wert | Value | Berechneter durchschnittlicher Stückwert eines Gegenstands. Konkrete Kaufpreise haben Vorrang vor dem Schätzwert. Ohne konkrete Kaufpreise entspricht der Wert dem konkreten Schätzwert. Ohne konkrete Kaufpreise und ohne konkreten Schätzwert ist kein Wert bestimmbar. |
| Schätzwert | Estimated Value | Direkt am Gegenstand gepflegte fachliche Einschätzung des Werts. Der Schätzwert kann auch neben konkreten Kaufpreisen existieren. Der Wert 0 bedeutet „unbekannt“. |
| Währung | Currency | Währung eines Geldwerts. Aktuell wird in der Anwendung immer EUR verwendet. |
| Bezugsquelle | Source | Quelle, aus der ein Gegenstand stammt oder bei der er gekauft wurde, z. B. physischer Laden, Webshop oder Flohmarkt. |
| Bezugsquellen-ID | Source ID | Interne eindeutige ID einer Bezugsquelle. |
| Bezugsquellenname | Source Name | Fachlich eindeutiger Name einer Bezugsquelle. Die Eindeutigkeit ist unabhängig von Groß- und Kleinschreibung und wird später im Application-Ring über den normalisierten Namen durchgesetzt. |
| Bezugsquellen-Details | Source Details | Optionale Freitextangabe an einer Bezugsquelle für Adresse, URL oder sonstige formlose Informationen. |
| Zugang | Acquisition | Fachlicher Vorgang, durch den Exemplare eines Gegenstands durch Kauf oder Geschenk hinzukommen. Dabei wird eine Gegenstand-Bezugsquelle-Zuordnung angelegt oder die Menge einer bestehenden fachlich gleichen Zuordnung erhöht. |
| Abgang | Removal | Fachlicher Vorgang, durch den Exemplare eines Gegenstands nicht mehr im Besitz sind. Der Grund ist fachlich irrelevant. Ein Abgang reduziert direkt die Ortsmenge und wird nicht als Historie dokumentiert. |
| Gegenstand-Bezugsquelle-Zuordnung | Item Source | Zuordnung eines Gegenstands zu einer Bezugsquelle. An dieser Zuordnung hängen Menge, Kaufpreis und Kaufdatum. Mehrere Zuordnungen zwischen demselben Gegenstand und derselben Bezugsquelle sind erlaubt, wenn sich Datum, Kaufpreis oder beides unterscheidet. Fachlich identische Zuordnungen sind nicht erlaubt. |
| Kaufpreis | Purchase Price | Preisangabe an der Zuordnung zwischen Gegenstand und Bezugsquelle. Kaufpreise beziehen sich immer auf ein Stück. Der Wert 0 bedeutet „unbekannt“ und nicht „kostenlos“. |
| Kaufdatum | Purchase Date | Datum des Kaufs, sofern bekannt. |
| Gesamtwert | Total Value | Gesamtwert eines Gegenstands. Er ergibt sich aus der Gesamtmenge an Orten und dem durchschnittlichen Stückwert. |
| Notiz | Note | Freitextfeld für zusätzliche Informationen zu einem Gegenstand. |

## Später geplant

- Betrieb auf einer Synology-NAS im Heimnetz
- Nutzung durch Jens und Irena
- Fotos
- PDF-Rechnungen
- Benutzerkonten
- Mobile Nutzung
- Auswertungen
