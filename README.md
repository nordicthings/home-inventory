# home-inventory
Simple inventory app as an training example for AI-based development

## Lokaler Betrieb

### H2

```bash
./gradlew bootRun
```

### MariaDB

Lokale Umgebung anlegen:

```bash
cp .env.example .env
mkdir -p .local/mariadb-data
```

MariaDB starten:

```bash
docker compose -f docker-compose.mariadb.yml up -d
```

App gegen MariaDB starten:

```bash
set -a
source .env
set +a
./gradlew bootRun --args='--spring.profiles.active=mariadb'
```

### MariaDB stoppen

```bash
docker compose -f docker-compose.mariadb.yml down
```

Mit `down -v` werden zusätzlich die lokalen Datenbank-Volumes gelöscht.

## Docker-Image der Anwendung

Das Script baut zuerst das Spring-Boot-JAR und erzeugt daraus lokal das Docker-Image. Der Container startet standardmäßig mit dem Spring-Profil `mariadb`.

### Image bauen:

```bash
./scripts/build-image.sh
```

Mit expliziter Versionsnummer:

```bash
./scripts/build-image.sh 1.1.0
```

Optional mit explizitem Namen und/oder Plattform (das Image wird zusätzlich IMMER mit `latest` getaggt):

```bash
IMAGE_NAME=home-inventory PLATFORM=linux/amd64 ./scripts/build-image.sh 1.1.0
```

- linux/amd64 ist für Intel-/AMD-Prozessoren
- linux/arm64 für ARM-Prozessoren (z.B. Apple Silicon).

Defaults sind:
- IMAGE_NAME=home-inventory
- IMAGE_TAG=0.1.0, sofern keine Versionsnummer übergeben wird
- PLATFORM=linux/amd64

Das Image wird dann in der lokalen Docker-Registry gespeichert.

Um es auf das NAS zu bringen, muss es aus der lokalen Registry exportiert werden:

```bash
./scripts/build-image.sh 1.1.0 --export
```

Das erzeugt zusätzlich die Datei `home-inventory-1.1.0.tar`. Mit `--export-dir <zielordner>` kann ein anderer Zielordner gewählt werden.

Das Image kann nun auf dem NAS geladen werden.

### Container lokal starten:
Sofern der Container für die lokale Host-Architektur passt, kann man ihn gegen eine vorhandene MariaDB starten. 
Die Umgebungsvariablen `DB_HOST`, `DB_USERNAME` und `DB_PASSWORD` müssen lokal angelegt werden.

```bash
docker run --rm \
  --name home-inventory \
  -p 8080:8080 \
  -e DB_HOST=$DB_HOST \
  -e DB_PORT=3306 \
  -e DB_NAME=home_inventory \
  -e DB_USERNAME=$DB_USERNAME \
  -e DB_PASSWORD=$DB_PASSWORD \
  home-inventory:1.1.0
```

Alternativ per Docker Compose, wenn die Datenbank bereits auf dem NAS läuft:

```bash
APP_IMAGE=home-inventory:1.1.0 \
DB_HOST=<nas-hostname-oder-ip> \
DB_PORT=3306 \
DB_NAME=home_inventory \
DB_USERNAME=<db-user> \
DB_PASSWORD=<db-passwort> \
docker compose -f docker-compose.app.yml up -d
```

Die Compose-Datei verwendet standardmäßig das feste Docker-Subnetz `172.30.10.0/24` und die feste Container-IP `172.30.10.10`. Der MariaDB-User kann dafür z. B. auf `'appuser'@'172.30.10.%'` oder enger auf `'appuser'@'172.30.10.10'` berechtigt werden.

Für hochgeladene Rechnungsdateien bindet die Compose-Datei standardmäßig `.local/files` als persistentes Volume nach `/data/home-inventory/files` ein. Auf dem NAS sollte `FILE_STORAGE_PATH` auf einen dauerhaft gesicherten Ordner gesetzt werden. Die maximale Uploadgröße kann über `HOME_INVENTORY_FILES_MAX_UPLOAD_SIZE` angepasst werden; der Standardwert ist `10MB`.

## Anlegen einer DB auf dem Synology NAS
Eine SSH-Session auf dem NAS starten. Bei Bedarf vorher den ssh-Dienst starten.

__Beachte:__
Das NAS lässt SSH-Verbindungen ausschließlich für Benutzer der Gruppe `Administrators` zu.

`ssh -l my-admin-user <my-nas-name>`

Danach mit mysql-Client auf dem NAS eine Verbindung herstellen:

`mysql -u my_admin_user -p`


### Datenbank anlegen
`create home_inventory;`

### Benutzer anlegen

Um aus dem lokalen Netz auf die produktive MariaDB zuzugreifen, wird ein DB-User für den entsprecdhenden IO-Adressbereich benötigt:

`create user 'appuser'@'192.168.178.%' identified by 'mein_passwort';`

Für den auf dem NAS laufenden Docker-Container ist ein weiterer DB-User erforderlich

`create user 'appuser'@'172.30.10.%' identified by 'mein_passwort';`

Anschließend dem Applikations-User die erforderlichen DDL-Rechte zuweisen. Beachte, dass die Rechte sowohl für den lokalen Netz-User als auch für den Container-User gesetzt werden müssen. Hier als Beispiel für den User aus dem lokalen Netz:

```sql
-- Struktur-Operationen auf allen Tabellen in einer DB
GRANT SELECT, DELETE, UPDATE, INSERT, CREATE, ALTER, DROP, INDEX, REFERENCES, CREATE TEMPORARY TABLES
ON home_inventory.*
TO 'appuser'@'192.168.178.%';

-- Views (für vollständige DDL-Abdeckung)
GRANT CREATE VIEW, SHOW VIEW
ON home_inventory.*
TO 'appuser'@'192.168.178.%';

-- Stored Programs (optional, falls du das brauchst)
GRANT CREATE ROUTINE, ALTER ROUTINE, EXECUTE
ON home_inventory.*
TO 'appuser'@'192.168.178.%';

FLUSH PRIVILEGES;
```
