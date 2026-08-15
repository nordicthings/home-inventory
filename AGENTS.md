# Projektanweisungen

## Fachliche Vorgaben

Die fachlichen Anforderungen stehen in `docs/requirements.md` und sind bei Umsetzung und Tests zu beachten.

## Technologiestack

- Backend: Kotlin mit Spring Boot
- UI: Serverseitig gerendertes HTML mit Thymeleaf
- Interaktivität: HTMX
- Buildtool: Gradle mit Kotlin DSL
- Datenbank in der Entwicklungsphase: H2
- Spätere Datenbank auf der Synology-NAS: PostgreSQL
- Datenbankmigrationen: Liquibase mit YAML-Changelogs
- Datenbankunabhängigkeit ist wichtig; H2-spezifische Funktionen und datenbankspezifisches SQL nach Möglichkeit vermeiden

## Lokaler Betrieb

- Die App soll lokal direkt aus der IDE oder per Gradle mit H2 ausführbar sein
- Die App soll zusätzlich lokal als Docker-Container per Docker Compose ausführbar sein
- Docker Compose soll den Betrieb gegen eine PostgreSQL-Datenbank ermöglichen
- Die Konfiguration soll über Spring-Profile getrennt werden, z. B. H2 für schnelle Entwicklung und PostgreSQL für containerisierten Betrieb
- Liquibase-Migrationen müssen in beiden lokalen Betriebsarten laufen

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

## Leitplanken

- Betrieb lokal, später nur im Heimnetz
- Keine Cloud
- Keine kostenpflichtigen Komponenten
- Keine Datenübertragung nach außen
- Vor Architektur-, Technologie- oder wesentlichen Funktionsentscheidungen mehrere Optionen mit Vor- und Nachteilen vorlegen und Zustimmung einholen
- Für neue Funktionen passende Tests und Prüfung vor Übergabe
