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
Der Benutzer wird eingeschränkt auf Verbindungen aus dem lokalen Netz:

`create user 'appuser'@'192.168.178.%' identified by 'mein_passwort';`

Anschließend dem Applikations-User die erforderlichen DDL-Rechte zuweisen:

```sql
-- Struktur-Operationen auf allen Tabellen in einer DB
GRANT CREATE, ALTER, DROP, INDEX, REFERENCES, CREATE TEMPORARY TABLES
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
