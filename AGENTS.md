# ROLLE:
Du bist Senior Softwarearchitekt.

# VERMEIDEN:
Wenn Informationen fehlen:
- keine Annahmen treffen
- Rückfragen stellen
- Unsicherheiten markieren
- 
# Projektanweisungen

## Fachliche Vorgaben

Die fachlichen Anforderungen stehen in `docs/requirements.md` und sind bei Umsetzung und Tests zu beachten.

## Technologiestack

- Backend: Kotlin mit Spring Boot
- UI: Serverseitig gerendertes HTML mit Thymeleaf
- Interaktivität: HTMX
- Buildtool: Gradle mit Kotlin DSL
- Datenbank in der Entwicklungsphase: H2
- Spätere Datenbank auf der Synology-NAS: MariaDB
- Datenbankmigrationen: Liquibase mit YAML-Changelogs
- Datenbankunabhängigkeit ist wichtig; H2-spezifische Funktionen und datenbankspezifisches SQL nach Möglichkeit vermeiden

## Lokaler Betrieb

- Die App soll lokal direkt aus der IDE oder per Gradle mit H2 ausführbar sein
- Die App soll zusätzlich lokal als Docker-Container per Docker Compose ausführbar sein
- Docker Compose soll den lokalen Betrieb gegen MariaDB ermöglichen
- Die Konfiguration soll über Spring-Profile getrennt werden, z. B. H2 für schnelle Entwicklung und MariaDB für produktionsnahen Betrieb
- Liquibase-Migrationen müssen in beiden lokalen Betriebsarten laufen
- Codex darf während der Bearbeitung einen Dev-Server starten, wenn das für UI-Prüfungen notwendig ist. Nach abgeschlossenem Arbeitsschritt startet Codex keinen Übergabe-Server, sondern nennt den passenden Startbefehl, damit der Nutzer den Dev-Server selbst kontrolliert.

## Architekturstil

- Die Anwendung wird als modularer Monolith umgesetzt
- Das Projekt bleibt zunächst ein Single-Module-Gradle-Projekt
- Die Modularisierung erfolgt über Packages
- Pro Fachmodul wird eine leichtgewichtige hexagonale Struktur verwendet
- Vorgesehene Package-Bereiche je Fachmodul: `domain`, `application`, `adapter/web`, `adapter/persistence`
- Die Domain bleibt frei von technischen Framework-Abhängigkeiten wie Spring, JPA, Thymeleaf oder HTMX
- Die Application-Schicht darf nicht von Adaptern abhängen
- Web- und Persistence-Adapter greifen nicht direkt aufeinander zu
- Die wichtigsten Abhängigkeitsregeln werden mit ArchUnit-Tests abgesichert
- jMolecules wird zur expliziten Kennzeichnung fachlicher und architektonischer Bausteine verwendet
- Domänenobjekte werden mit passenden jMolecules-DDD-Annotationen wie `@AggregateRoot`, `@Entity` und `@ValueObject` markiert
- Die Identität von `@Entity`- und `@AggregateRoot`-Typen wird mit `@Identity` markiert
- Im Domänenkern steht die führende fachliche Klassendeklaration einer Datei direkt nach Package, Imports und Annotationen oben; unterstützende IDs, Namen, Value Objects oder Enums folgen darunter
- Beim späteren Einführen der strukturellen Ringe werden auch `domain`, `application` und `adapter` mit passenden jMolecules-Architekturannotationen markiert
- jMolecules-Annotationen dokumentieren die Architekturabsicht; sie ersetzen nicht die Package-Struktur oder ArchUnit-Tests

## Leitplanken

- Betrieb lokal, später nur im Heimnetz
- Keine Cloud
- Keine kostenpflichtigen Komponenten
- Keine Datenübertragung nach außen
- Vor Architektur-, Technologie- oder wesentlichen Funktionsentscheidungen mehrere Optionen mit Vor- und Nachteilen vorlegen und Zustimmung einholen
- Für neue Funktionen passende Tests und Prüfung vor Übergabe

## Teststrategie

- Fachliche Funktionalitäten im Domain Model werden mit Unit-Test abgesichert.
- Es wird nicht nur der Happy Path getestet, sondern auch Edge-Cases, wie z.B. leerer/falscher Input
- Funktionalitäten im Application-Ring werden unter Zuhilfenahme von Mocks getestet.
- Der Schnitt der Testcode-Dateien orientiert sich am Schnitt des produktiven Codes. 
