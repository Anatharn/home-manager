-- =====================================================================
-- Home Butler — création de la base MariaDB
--
-- Reproduit le schéma généré par Hibernate (ddl-auto: update) à partir
-- des entités JPA de backend/src/main/java/sds/home/homebutler/model/,
-- pour permettre de préparer la base MariaDB à l'avance plutôt que de
-- laisser Hibernate la créer au premier démarrage.
--
-- Utilisation :
--   mysql -u root -p < mariadb-schema.sql
--
-- Adapter le nom de la base, l'utilisateur et le mot de passe ci-dessous
-- avant exécution, puis reporter les mêmes valeurs dans la vue
-- Configuration > Base de données de l'application.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `home_butler`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'home_butler_user'@'%' IDENTIFIED BY 'change_me';
GRANT ALL PRIVILEGES ON `home_butler`.* TO 'home_butler_user'@'%';
FLUSH PRIVILEGES;

USE `home_butler`;

-- ---------------------------------------------------------------------
-- banque
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `banque` (
    `id`  BIGINT NOT NULL AUTO_INCREMENT,
    `nom` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- type_compte
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `type_compte` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `libelle` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- categorie
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `categorie` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT,
    `libelle` VARCHAR(255) NOT NULL,
    `couleur` VARCHAR(255) NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- format_import_csv
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `format_import_csv` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `nom`           VARCHAR(255) NOT NULL,
    -- JSON (délimiteur, séparateur décimal, format de date, mapping des colonnes...), voir
    -- frontend/src/utils/formatsImportCsv.js pour la structure — opaque pour le backend.
    `configuration` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `uk_format_import_csv_nom` UNIQUE (`nom`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- compte
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `compte` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `libelle`        VARCHAR(255) NOT NULL,
    `iban`           VARCHAR(255) NULL,
    `code`           VARCHAR(255) NOT NULL,
    `solde`          DECIMAL(12, 2) NULL,
    `banque_id`      BIGINT NULL,
    `type_compte_id` BIGINT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_compte_banque`
        FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`),
    CONSTRAINT `fk_compte_type_compte`
        FOREIGN KEY (`type_compte_id`) REFERENCES `type_compte` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- transaction (nom entre backticks : c'est un mot réservé en SQL)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `transaction` (
    `id`             BIGINT NOT NULL AUTO_INCREMENT,
    `libelle`        VARCHAR(255) NOT NULL,
    `montant`        DECIMAL(12, 2) NOT NULL,
    `date_operation` DATE NOT NULL,
    `compte_id`      BIGINT NOT NULL,
    `categorie_id`   BIGINT NULL,
    `commentaire`    VARCHAR(2000) NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_transaction_compte`
        FOREIGN KEY (`compte_id`) REFERENCES `compte` (`id`),
    CONSTRAINT `fk_transaction_categorie`
        FOREIGN KEY (`categorie_id`) REFERENCES `categorie` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- cheque
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `cheque` (
    `id`                  BIGINT NOT NULL AUTO_INCREMENT,
    `numero`              VARCHAR(255) NOT NULL,
    `beneficiaire`        VARCHAR(255) NULL,
    `montant`             DECIMAL(12, 2) NOT NULL,
    `date_emission`       DATE NOT NULL,
    `date_encaissement`   DATE NULL,
    -- Reflète l'enum StatutCheque (EMIS, ENCAISSE, ANNULE), persisté par Hibernate
    -- en VARCHAR (EnumType.STRING) : le CHECK ci-dessous n'est qu'une sécurité
    -- côté base, sans incidence sur le contrôle de schéma de Hibernate.
    `statut`              VARCHAR(255) NOT NULL,
    `compte_id`           BIGINT NOT NULL,
    `transaction_id`      BIGINT NULL,
    `transaction_generee` BOOLEAN NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_cheque_compte`
        FOREIGN KEY (`compte_id`) REFERENCES `compte` (`id`),
    CONSTRAINT `fk_cheque_transaction`
        FOREIGN KEY (`transaction_id`) REFERENCES `transaction` (`id`),
    CONSTRAINT `chk_cheque_statut`
        CHECK (`statut` IN ('EMIS', 'ENCAISSE', 'ANNULE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
