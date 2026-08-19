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

Mit `down -v` werden zusaetzlich die lokalen Datenbank-Volumes geloescht.
