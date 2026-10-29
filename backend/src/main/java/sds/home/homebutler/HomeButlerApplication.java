package sds.home.homebutler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@SpringBootApplication
public class HomeButlerApplication {

    // Chemin écrit par DatabaseConfigService depuis la vue Configuration > Base de données.
    private static final Path FICHIER_CONFIG_DATASOURCE = Path.of("config", "datasource.properties");

    public static void main(String[] args) {
        // Vérifié avant même de démarrer le contexte Spring : en profil "prod", H2 restant sur
        // le classpath (utilisé par le profil "dev"), Spring Boot basculerait sinon
        // silencieusement sur une base H2 en mémoire tant qu'aucune "spring.datasource.url"
        // n'est fournie, au lieu d'échouer clairement comme voulu (voir application-prod.yml).
        if (isProfileProdActif(args) && Files.notExists(FICHIER_CONFIG_DATASOURCE)) {
            System.err.println(
                    "Aucune base de données configurée : le profil \"prod\" exige une connexion "
                            + "renseignée dans " + FICHIER_CONFIG_DATASOURCE.toAbsolutePath()
                            + " (voir la vue Configuration > Base de données de l'application).");
            System.exit(1);
        }

        SpringApplication.run(HomeButlerApplication.class, args);
    }

    private static boolean isProfileProdActif(String[] args) {
        return contientProd(System.getenv("SPRING_PROFILES_ACTIVE"))
                || contientProd(System.getProperty("spring.profiles.active"))
                || Arrays.stream(args)
                        .filter(arg -> arg.startsWith("--spring.profiles.active="))
                        .anyMatch(arg -> contientProd(arg.substring(arg.indexOf('=') + 1)));
    }

    private static boolean contientProd(String profils) {
        return profils != null && Arrays.asList(profils.split(",")).contains("prod");
    }

}
