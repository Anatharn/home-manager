# Home Butler - Gestion de comptes bancaires personnels

Application desktop de gestion de comptes bancaires personnels.

- **Frontend** : Tauri + Vue (dossier `frontend/`, à venir)
- **Backend** : Spring Boot + Maven (dossier `backend/`)
- **Base de données** : H2 (fichier local) en développement, MariaDB (distante) en production — voir profils Spring ci-dessous

## Backend

### Prérequis
- Java 25 (LTS)
- Maven (ou utiliser le wrapper `mvnw` si ajouté)

> **Note (10/09/2026)** : le projet utilisait Lombok pour générer les accesseurs des entités, mais sous Java 25 l'annotation processing s'est montré non fiable (accesseurs manquants de façon intermittente, sans erreur de compilation visible — voir historique). Lombok a donc été retiré du projet : les entités déclarent désormais leurs getters/setters/constructeurs explicitement, et le projet cible Java 25 sans restriction.

### Configuration — profils Spring
Le profil actif par défaut est `dev` (voir `application.yml`) : aucune configuration n'est nécessaire, la base H2 est stockée dans un fichier local (`backend/data/home-butler.mv.db`), créé automatiquement au premier démarrage.

Le profil `prod` (utilisé par le jar packagé pour l'app Tauri, voir `spring_launcher.rs`) n'a aucune base par défaut : la connexion MariaDB doit être renseignée depuis la vue Configuration > Base de données de l'app (ou en écrivant directement `backend/config/datasource.properties`, voir `DatabaseConfigService`). Sans cette configuration, le démarrage en profil `prod` échoue volontairement — c'est voulu, pour éviter qu'un déploiement sans base configurée ne bascule silencieusement sur H2.

Pour lancer le backend seul avec le profil `prod` (ex. pour tester la connexion MariaDB sans passer par Tauri) :

```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

### Lancer le backend
Depuis le dossier `backend/` :

```bash
mvn spring-boot:run
```

L'API sera disponible sur `http://localhost:8080/api`.

La console H2 (visualiser les tables et données) est accessible sur `http://localhost:8080/h2-console` :
- JDBC URL : `jdbc:h2:file:./data/home-butler`
- Utilisateur : `sa`
- Mot de passe : (vide)

### Structure du package
```
sds.home.homebutler
├── model/         # Entités JPA (Compte, Transaction, Categorie, Banque)
├── compte/        # Controller, Service, Repository - Comptes
├── transaction/   # Controller, Service, Repository - Transactions
├── categorie/     # Controller, Service, Repository - Catégories
└── config/        # Configuration (CORS, etc.)
```

### Endpoints disponibles
- `GET/POST /api/comptes`, `GET/PUT/DELETE /api/comptes/{id}`
- `GET/POST /api/transactions` (filtrable par `?compteId=`), `GET/PUT/DELETE /api/transactions/{id}`
- `GET/POST /api/categories`, `GET/PUT/DELETE /api/categories/{id}`
- `GET/POST /api/banques`, `GET/PUT/DELETE /api/banques/{id}`
- `GET/POST /api/cheques` (filtrable par `?compteId=`), `GET/PUT/DELETE /api/cheques/{id}` — cycle de vie Émis → Encaissé → Annulé :
  - `PUT` refusé (409) une fois le chèque encaissé ou annulé
  - `POST /api/cheques/{id}/encaisser` (`{ "dateEncaissement": "...", "categorieId": ... }`) génère automatiquement la transaction de débit liée
  - `POST /api/cheques/{id}/lier-transaction` (`{ "transactionId": ... }`) rattache le chèque à une transaction déjà existante du même compte (ex. importée) au lieu d'en générer une nouvelle — refusé (409) si la transaction appartient à un autre compte ou est déjà liée à un autre chèque
  - `POST /api/cheques/{id}/annuler` rompt le lien vers la transaction ; celle-ci n'est supprimée que si elle avait été générée par `encaisser` (jamais si liée via `lier-transaction`) — idempotent

## Frontend

### Prérequis
- Node.js 18+
- Rust (via [rustup](https://rustup.rs)) et les prérequis système de Tauri ([voir la doc officielle](https://v2.tauri.app/start/prerequisites/))
- Java 25 installé et accessible dans le `PATH` (nécessaire pour lancer le jar Spring)

### Développement
Le backend Spring doit être packagé en jar avant de lancer l'app en dev, car Tauri le démarre lui-même comme process enfant :

```bash
cd backend
mvn clean package
cp target/home-butler.jar ../frontend/src-tauri/resources/backend.jar
```

Puis, depuis `frontend/` :

```bash
npm install
npm run tauri dev
```

Ça lance Vite (port 1420) et la fenêtre Tauri, qui démarre elle-même le jar Spring en arrière-plan (voir `src-tauri/src/spring_launcher.rs`). Le backend est arrêté automatiquement à la fermeture de la fenêtre.

### Build complet (backend + frontend)
Le script `scripts/build-all.sh` automatise le build du jar, sa copie dans les ressources Tauri, puis le build de l'app :

```bash
./scripts/build-all.sh
```

Le binaire final se trouve dans `frontend/src-tauri/target/release/`.

### Structure
```
frontend/
├── src/                  # App Vue
│   ├── views/            # ComptesView, TransactionsView
│   ├── components/       # CompteCard
│   ├── stores/           # Pinia (comptes, transactions)
│   ├── api/              # Client axios -> http://localhost:8080/api
│   └── router/
└── src-tauri/            # Partie Rust/Tauri
    ├── src/
    │   ├── main.rs             # Lance/arrête le backend Spring
    │   └── spring_launcher.rs
    ├── resources/backend.jar   # Jar Spring (à copier avant build, non versionné)
    └── tauri.conf.json
```

