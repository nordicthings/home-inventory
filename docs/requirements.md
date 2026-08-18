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

## Version 2

Für Version 2 vorgemerkt:

- Numerische Eingabefelder sollen falsche Zeichen möglichst bereits während der Eingabe verhindern.
- Geldwerte sollen clientseitig nur im deutschen Zahlenformat erfasst werden können, z. B. `1.234,56`.
- Ganzzahlige Mengen sollen clientseitig nur als positive ganze Zahlen mit optionalem deutschem Tausenderpunkt erfasst werden können, z. B. `1.000`.
- Serverseitige Validierung bleibt unabhängig davon weiterhin verbindlich.

## Felder

Jeder Gegenstand hat diese Felder:

- Name
- Kategorie
- Schätzwert
- Freie Notiz

Name und Kategorie sind Pflichtfelder. Der Schätzwert ist optional. Wenn kein Schätzwert erfasst wird, wird 0 verwendet. Die Notiz ist optional.

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

Ortsmengen sind direkt änderbar.

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

Mehrere Zuordnungen zwischen demselben Gegenstand und derselben Bezugsquelle sind erlaubt, wenn sich Datum, Kaufpreis oder beides unterscheiden.

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

Kategorien gehören zu den Stammdaten bzw. Strukturdaten.

Jede Kategorie hat eine interne eindeutige ID. IDs werden in der Domäne grundsätzlich als UUID modelliert.

Kategorien sind pflegbar. Sie können in der Anwendung angelegt, bearbeitet und gelöscht werden.

Kategorien können umbenannt werden. Die Zuordnung bestehender Gegenstände zu dieser Kategorie bleibt dabei erhalten.

Kategorienamen sind eindeutig.

Die Eindeutigkeit von Kategorienamen ist unabhängig von Groß- und Kleinschreibung. Die Durchsetzung dieser Invariante erfolgt im Application-Ring über den normalisierten Kategorienamen.

Kategorien klassifizieren Gegenstände hinsichtlich ihres Gebrauchs, z. B. Möbel, Unterhaltungselektronik oder Computer & Peripherie.

Eine Kategorie hat außer ihrem Namen keine weiteren fachlichen Eigenschaften.

Jeder Gegenstand hat genau eine Kategorie.

Eine Kategorie, der noch Gegenstände zugeordnet sind, kann nicht gelöscht werden.

`Sonstiges` ist eine Kategorie wie alle anderen auch.

## Start-Bezugsquellen

- Amazon
- Lüchau
- Toom
- Euronics
- rsMöbel
- Mediamarkt
- Saturn
- Sonstige

Die genannten Bezugsquellen sind Startwerte.

## Startdaten

Initiale Orte, Kategorien und Bezugsquellen werden über ein Liquibase- oder SQL-Skript angelegt.

## Use Cases

### Gegenstand anlegen

Ein Gegenstand wird mit den zentralen Attributen Name und Kategorie angelegt. Der Schätzwert und die Notiz sind optional.

Beim Anlegen muss eine existierende Kategorie ausgewählt werden.

Beim Anlegen muss der normalisierte Gegenstandsname eindeutig sein.

Für eine effiziente Erfassung kann beim Anlegen optional direkt ein Zugang und/oder ein Ortsbestand miterfasst werden.

Wenn beim Anlegen ein Ortsbestand miterfasst wird, müssen Ort und Menge angegeben werden. Die Menge muss größer als 0 sein.

Wenn beim Anlegen ein Zugang miterfasst wird, gelten dieselben Regeln wie beim separaten Erfassen eines Zugangs.

Wenn beim Anlegen Ortsbestand und Zugang miterfasst werden und die Gesamtmenge der Zugänge die aktuelle Gesamtmenge an Orten übersteigt, wird der Benutzer darauf hingewiesen.

Ein Zugang beim Anlegen ist optional. Viele Gegenstände werden insbesondere bei der initialen Inventarisierung ohne Zugang erfasst, wenn ihre Bezugsquelle nicht mehr bekannt ist.

Ein Zugang ohne Bezugsquelle ist nicht erlaubt.

Es wird keine künstliche Bezugsquelle wie `Unbekannt` verwendet, nur um unbekannte Bezugsquellen abzubilden.

### Gegenstand bearbeiten

Beim Bearbeiten eines Gegenstands können Name, Kategorie, Schätzwert und Notiz geändert werden.

Beim Umbenennen eines Gegenstands muss der normalisierte neue Name eindeutig sein. Der bearbeitete Gegenstand selbst wird dabei nicht als Konflikt betrachtet.

Beim Ändern der Kategorie muss eine existierende Kategorie ausgewählt werden.

Der Schätzwert muss größer oder gleich 0 sein.

Ortsbestände und Zugänge werden über eigene Use Cases gepflegt.

### Gegenstand löschen

Das Löschen eines Gegenstands muss vom Benutzer bestätigt werden.

Beim Löschen eines Gegenstands werden alle Zuordnungen dieses Gegenstands entfernt.

Orte und Bezugsquellen bleiben beim Löschen eines Gegenstands bestehen, auch wenn sie danach keine Zuordnungen mehr haben.

Ein Gegenstand kann unabhängig von Ortsmengen und Zugängen gelöscht werden.

Das Löschen eines Gegenstands ist endgültig. Es gibt keinen Papierkorb und kein Soft Delete.

### Ortsbestand setzen

Beim Ortsbestand setzen wird für einen Gegenstand die absolute Menge an einem Ort gesetzt.

Wird die Menge auf 0 gesetzt, wird die Ortszuordnung entfernt.

Wenn für einen Gegenstand an einem Ort noch keine Ortszuordnung existiert und ein Ortsbestand neu gesetzt wird, muss die Menge größer als 0 sein.

Eine Ortszuordnung mit Menge 0 ist nicht erlaubt.

Wenn beim Setzen eines neuen Ortsbestands die Menge 0 angegeben wird, erhält der Benutzer eine Fehlermeldung.

Ortsbestand setzen ist eine Korrektur- bzw. Erfassungsfunktion. Dabei darf der Bestand auf einen beliebigen Wert größer oder gleich 0 gesetzt werden, sofern die Regeln für neue Ortszuordnungen eingehalten werden.

Wenn nach dem Setzen eines Ortsbestands die Gesamtmenge der Zugänge die aktuelle Gesamtmenge an Orten übersteigt, wird der Benutzer darauf hingewiesen.

Wenn die Gesamtmenge der Zugänge kleiner ist als die aktuelle Gesamtmenge an Orten, wird dies nicht kommentiert.

### Abgang erfassen

Ein Abgang reduziert die Menge eines Gegenstands an einem Ort.

Beim Erfassen eines Abgangs wird immer der Ort angegeben, an dem die Menge reduziert wird.

Wenn die Abgangsmenge größer ist als die Menge am angegebenen Ort, ist dies ein fachlicher Fehler.

Der Grund des Abgangs wird nicht erfasst.

Abgänge und Zugänge sind fachlich unabhängig. Ein Abgang verändert keine Zugänge.

Wenn nach einem Abgang die Gesamtmenge der Zugänge die aktuelle Gesamtmenge an Orten übersteigt, wird der Benutzer darauf hingewiesen.

### Umlagerung erfassen

Eine Umlagerung verschiebt Exemplare eines Gegenstands von einem Quellort an einen Zielort.

Für eine Umlagerung werden Gegenstand, Quellort, Zielort und Menge angegeben.

Quellort und Zielort müssen unterschiedlich sein.

Quellort und Zielort müssen existieren.

Die Umlagerungsmenge darf die aktuelle Menge am Quellort nicht überschreiten.

Wenn für den Zielort noch keine Ortszuordnung existiert, wird sie durch die Umlagerung angelegt.

Wenn die Menge am Quellort durch die Umlagerung auf 0 fällt, wird die Quell-Ortszuordnung gelöscht.

Die Änderung am Quellort und die Änderung am Zielort erfolgen atomar.

### Zugang erfassen

Ein Zugang erfasst, dass Exemplare eines Gegenstands über eine Bezugsquelle hinzugekommen sind.

Für einen Zugang werden Gegenstand, Bezugsquelle und Menge angegeben.

Kaufdatum und Kaufpreis werden ebenfalls erfasst; beide Angaben sind optional. Ein unbekannter Kaufpreis wird mit 0 abgebildet.

Ein Zugang verändert keine Ortsmengen.

Wenn beim Erfassen eines Zugangs eine fachlich gleiche Gegenstand-Bezugsquelle-Zuordnung bereits existiert, wird deren Menge erhöht. Der Benutzer erhält darüber einen kurzen informativen Hinweis.

Bestehende Zugänge können nachträglich bearbeitet werden, um Fehler zu korrigieren. Bearbeitbar sind Bezugsquelle, Menge, Kaufpreis und Kaufdatum.

Wenn ein Zugang so bearbeitet wird, dass er fachlich gleich zu einem anderen Zugang desselben Gegenstands wird, werden die Mengen beider Zugänge zusammengeführt.

Beim Bearbeiten eines Zugangs darf die Bezugsquelle geändert werden. Die neue Bezugsquelle muss existieren.

Beim Bearbeiten eines Zugangs darf der Kaufpreis leer gelassen werden; intern wird dann 0 gespeichert.

Beim Bearbeiten eines Zugangs darf das Kaufdatum entfernt werden.

Die Menge eines Zugangs darf beim Bearbeiten nicht auf 0 gesetzt werden. Wenn ein Zugang gelöscht werden soll, muss dies über den dedizierten Lösch-Use-Case erfolgen.

Bestehende Zugänge können gelöscht werden. Das ist auch erlaubt, wenn aktuell Ortsmengen für den Gegenstand existieren.

Das Löschen eines Zugangs muss vom Benutzer bestätigt werden.

Wenn durch das Löschen eines Zugangs keine Zugänge mehr existieren, aber noch Ortsbestände vorhanden sind, erfolgt kein Hinweis.

### Stammdaten pflegen

Orte, Bezugsquellen und Kategorien können angelegt, bearbeitet und gelöscht werden.

Beim Anlegen eines Orts sind Name und Ortstyp Pflichtfelder.

Beim Bearbeiten eines Orts können Name und Ortstyp geändert werden.

Beim Anlegen oder Umbenennen eines Orts muss der normalisierte Ortsname eindeutig sein.

Ein Ort kann nur gelöscht werden, wenn keine Ortszuordnung mehr existiert.

Die initial angelegten Orte unterscheiden sich fachlich nicht von später manuell angelegten Orten.

Beim Anlegen einer Kategorie ist der Name Pflichtfeld.

Kategorien können umbenannt werden. Die Zuordnung bestehender Gegenstände bleibt erhalten.

Beim Anlegen oder Umbenennen einer Kategorie muss der normalisierte Kategoriename eindeutig sein.

Eine Kategorie kann nur gelöscht werden, wenn kein Gegenstand dieser Kategorie zugeordnet ist.

Beim Anlegen einer Bezugsquelle ist der Name Pflichtfeld. Details sind optional.

Beim Bearbeiten einer Bezugsquelle können Name und Details geändert werden.

Beim Anlegen oder Umbenennen einer Bezugsquelle muss der normalisierte Bezugsquellenname eindeutig sein.

Eine Bezugsquelle kann nur gelöscht werden, wenn keine Gegenstand-Bezugsquelle-Zuordnung existiert.

Wenn ein Stammdatensatz nicht gelöscht werden kann, reicht eine einfache Fehlermeldung.

Das Löschen von Orten, Kategorien und Bezugsquellen muss vom Benutzer bestätigt werden.

### Gegenstände suchen und filtern

Gegenstände können nach Name, Ort, Bezugsquelle und Kategorie gefiltert werden.

Die Namenssuche ist eine Enthält-Suche auf dem normalisierten Gegenstandsnamen. Der Suchbegriff wird ebenfalls normalisiert.

Wildcards werden vorerst nicht unterstützt.

Mehrere Filter werden mit UND verknüpft.

Der Filter nach Ort findet Gegenstände, die aktuell eine Menge größer als 0 an diesem Ort haben.

Der Filter nach Bezugsquelle findet Gegenstände mit mindestens einem Zugang zu dieser Bezugsquelle, unabhängig von Ortsmengen.

Der Filter nach Kategorie verwendet die Kategorie-ID.

### Hauptliste anzeigen

Die Hauptliste zeigt Gegenstände mit folgenden Spalten:

- Name
- Kategorie
- Gesamtmenge
- Durchschnittlicher Stückwert
- Gesamtwert

Die Hauptliste ist standardmäßig alphabetisch nach Name sortiert.

Gegenstände mit Gesamtmenge 0 werden in der Hauptliste angezeigt.

Wenn kein durchschnittlicher Stückwert oder Gesamtwert bestimmbar ist, zeigt die UI `unbekannt`.

### Gegenstandsdetails anzeigen

Die Detailansicht eines Gegenstands zeigt:

- zentrale Gegenstandsdaten
- Ortsbestände
- Zugänge
- berechneter durchschnittlicher Stückwert
- berechneter Gesamtwert

### Hinweise und Fehler

Hinweise blockieren den jeweiligen Use Case nicht.

Hinweise werden nach Abschluss des jeweiligen Use Cases angezeigt.

Fachliche Fehler werden unterscheidbar modelliert.

Folgende fachliche Fehler sind für Version 1 relevant:

- Name bereits vergeben: Beim Anlegen oder Umbenennen von Gegenstand, Ort, Kategorie oder Bezugsquelle existiert der normalisierte Name bereits.
- Menge zu groß: Abgang oder Umlagerung überschreitet die aktuelle Menge am Quellort.
- Ungültige Menge: Eine Menge verletzt die fachlichen Regeln, z. B. Zugang 0, neuer Ortsbestand 0 oder negative Menge.
- Stammdatensatz wird noch verwendet: Ort, Kategorie oder Bezugsquelle soll gelöscht werden, wird aber noch verwendet.

Folgende Situationen sind keine fachlichen Fehler:

- Eine Suche oder Filterung findet keine Gegenstände. In diesem Fall ist die Ergebnisliste leer.
- Eine referenzierte Kategorie, ein referenzierter Ort oder eine referenzierte Bezugsquelle fehlt nicht während der normalen Bedienung, da diese Werte ausschließlich aus Stammdaten ausgewählt werden.
- Ein Abgang oder eine Umlagerung von einem nicht zugeordneten Ort ist während der normalen Bedienung nicht möglich, da dafür eine bestehende Ortszuordnung ausgewählt wird.

Folgende Hinweise sind für Version 1 relevant:

- Zugang wurde zusammengeführt: Beim Erfassen oder Bearbeiten eines Zugangs wurde eine fachlich gleiche Gegenstand-Bezugsquelle-Zuordnung gefunden und die Menge zusammengeführt.
- Zugangsgesamtmenge übersteigt Ortsgesamtmenge: Die Gesamtmenge der Zugänge ist größer als die aktuelle Gesamtmenge an Orten.

### Use-Case-Namen

Die Use Cases werden in der Anwendung einheitlich mit verb-orientierten englischen Namen bezeichnet.

Für Version 1 sind folgende Use Cases vorgesehen:

- `CreateItem`: Gegenstand anlegen, optional mit initialem Ortsbestand und/oder initialem Zugang.
- `UpdateItem`: Name, Kategorie, Schätzwert und Notiz eines Gegenstands ändern.
- `DeleteItem`: Gegenstand inklusive Zuordnungen löschen.
- `GetItemDetails`: Detailansicht eines Gegenstands mit Ortsbeständen und Zugängen laden.
- `SearchItems`: Hauptliste mit Filtern nach Name, Ort, Bezugsquelle und Kategorie laden.
- `SetItemLocationQuantity`: Absolute Menge eines Gegenstands an einem Ort setzen.
- `RemoveFromLocation`: Menge eines Gegenstands an einem Ort durch Abgang reduzieren.
- `RelocateItem`: Menge eines Gegenstands von einem Quellort an einen Zielort umlagern.
- `AddItemAcquisition`: Zugang eines Gegenstands erfassen.
- `UpdateItemAcquisition`: Zugang eines Gegenstands bearbeiten.
- `DeleteItemAcquisition`: Zugang eines Gegenstands löschen.
- `CreateLocation`: Ort anlegen.
- `UpdateLocation`: Ort bearbeiten.
- `DeleteLocation`: Ort löschen.
- `CreateCategory`: Kategorie anlegen.
- `UpdateCategory`: Kategorie bearbeiten.
- `DeleteCategory`: Kategorie löschen.
- `CreateSource`: Bezugsquelle anlegen.
- `UpdateSource`: Bezugsquelle bearbeiten.
- `DeleteSource`: Bezugsquelle löschen.
- `GetLocationList`: Orte alphabetisch sortiert laden.
- `GetCategoryList`: Kategorien alphabetisch sortiert laden.
- `GetSourceList`: Bezugsquellen alphabetisch sortiert laden.

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
| Kategorie | Category | Stammdatum zur fachlichen Einordnung eines Gegenstands hinsichtlich seines Gebrauchs, z. B. Möbel, Unterhaltungselektronik oder Computer & Peripherie. Jeder Gegenstand hat genau eine Kategorie. |
| Kategorie-ID | Category ID | Interne eindeutige ID einer Kategorie. |
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
