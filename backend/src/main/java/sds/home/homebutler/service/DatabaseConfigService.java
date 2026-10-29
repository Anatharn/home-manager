package sds.home.homebutler.service;

import org.springframework.stereotype.Service;
import sds.home.homebutler.dto.DatabaseConnexionConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DatabaseConfigService {

    private static final Path FICHIER_CONFIG = Path.of("config", "datasource.properties");
    private static final Pattern URL_MARIADB = Pattern.compile("jdbc:mariadb://([^:/]+):(\\d+)/(.+)");

    public DatabaseConnexionConfig lire() {
        DatabaseConnexionConfig config = new DatabaseConnexionConfig();
        if (!Files.exists(FICHIER_CONFIG)) {
            return config;
        }

        Properties proprietes = charger();
        Matcher matcher = URL_MARIADB.matcher(proprietes.getProperty("spring.datasource.url", ""));
        if (matcher.matches()) {
            config.setHote(matcher.group(1));
            config.setPort(Integer.valueOf(matcher.group(2)));
            config.setNomBase(matcher.group(3));
        }
        config.setUtilisateur(proprietes.getProperty("spring.datasource.username"));
        config.setConfiguree(true);
        // Le mot de passe n'est jamais renvoyé au client, même s'il est déjà enregistré.
        return config;
    }

    public DatabaseConnexionConfig enregistrer(DatabaseConnexionConfig config) {
        if (isBlank(config.getHote()) || isBlank(config.getNomBase()) || isBlank(config.getUtilisateur())) {
            throw new IllegalArgumentException("Hôte, nom de la base et utilisateur sont obligatoires");
        }
        int port = config.getPort() != null ? config.getPort() : 3306;

        String motDePasse = config.getMotDePasse();
        if (isBlank(motDePasse) && Files.exists(FICHIER_CONFIG)) {
            // Le formulaire n'exige pas de ressaisir le mot de passe à chaque enregistrement :
            // s'il est laissé vide, on conserve celui déjà stocké dans le fichier.
            motDePasse = charger().getProperty("spring.datasource.password", "");
        }

        Properties proprietes = new Properties();
        proprietes.setProperty("spring.datasource.url",
                "jdbc:mariadb://%s:%d/%s".formatted(config.getHote(), port, config.getNomBase()));
        proprietes.setProperty("spring.datasource.username", config.getUtilisateur());
        proprietes.setProperty("spring.datasource.password", motDePasse != null ? motDePasse : "");
        proprietes.setProperty("spring.datasource.driver-class-name", "org.mariadb.jdbc.Driver");
        proprietes.setProperty("spring.jpa.database-platform", "org.hibernate.dialect.MariaDBDialect");
        // Le schéma est géré manuellement (voir scripts/mariadb-schema.sql), pas par Hibernate :
        // "update"/"validate" ouvriraient une connexion bloquante au démarrage, empêchant l'appli
        // de démarrer (et donc d'accéder à cette page pour corriger la config) si la base configurée
        // est injoignable.
        proprietes.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        // Délai court plutôt que les 30s par défaut de HikariCP : si la base est injoignable (mauvaise
        // adresse, pare-feu qui droppe les paquets...), le démarrage ne doit pas rester bloqué dessus.
        proprietes.setProperty("spring.datasource.hikari.connection-timeout", "5000");

        try {
            Files.createDirectories(FICHIER_CONFIG.getParent());
            try (var writer = Files.newBufferedWriter(FICHIER_CONFIG)) {
                proprietes.store(writer, "Généré par la vue Configuration > Base de données de Home Butler");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible d'écrire la configuration de la base de données", e);
        }

        return lire();
    }

    private Properties charger() {
        Properties proprietes = new Properties();
        try (var reader = Files.newBufferedReader(FICHIER_CONFIG)) {
            proprietes.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lire la configuration de la base de données", e);
        }
        return proprietes;
    }

    private boolean isBlank(String valeur) {
        return valeur == null || valeur.isBlank();
    }

}
