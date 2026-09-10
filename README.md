# Home Butler - Gestion de comptes bancaires personnels

Application desktop de gestion de comptes bancaires personnels.

- **Frontend** : Tauri + Vue (dossier `frontend/`, à venir)
- **Backend** : Spring Boot + Maven (dossier `backend/`)
- **Base de données** : H2 (fichier local, pour le POC) — bascule prévue vers MariaDB (distante, sur NAS) une fois le POC validé

## Backend

### Prérequis
- Java 25 (LTS)
- Maven (ou utiliser le wrapper `mvnw` si ajouté)

> **Note (10/09/2026)** : le projet utilisait Lombok pour générer les accesseurs des entités, mais sous Java 25 l'annotation processing s'est montré non fiable (accesseurs manquants de façon intermittente, sans erreur de compilation visible — voir historique). Lombok a donc été retiré du projet : les entités déclarent désormais leurs getters/setters/constructeurs explicitement, et le projet cible Java 25 sans restriction.

### Configuration
Pour le POC, aucune configuration n'est nécessaire : la base H2 est stockée dans un fichier local (`backend/data/home-butler.mv.db`), créé automatiquement au premier démarrage.

Pour basculer plus tard vers MariaDB, décommenter le bloc `spring.datasource` / `spring.jpa` correspondant en bas de `application.yml` et supprimer (ou commenter) le bloc H2.

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

