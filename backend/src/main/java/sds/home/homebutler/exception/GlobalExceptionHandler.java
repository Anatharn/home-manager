package sds.home.homebutler.exception;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduit les échecs de connexion à la base (base injoignable, identifiants invalides...) en une
// réponse claire pour le frontend, plutôt que la trace Hibernate/HikariCP brute par défaut. Ne
// catche volontairement que les exceptions de connectivité (pas DataAccessException au sens large,
// qui couvrirait aussi des erreurs métier comme une contrainte violée) : voir CompteService,
// TransactionService... dont les repositories déclenchent ces exceptions quand la base configurée
// (voir DatabaseConfigService) est injoignable.
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message) {
    }

    @ExceptionHandler({CannotCreateTransactionException.class, DataAccessResourceFailureException.class})
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(Exception e) {
        ErrorResponse body = new ErrorResponse(
                "database_unavailable",
                "Impossible de se connecter à la base de données. Vérifiez la configuration dans "
                        + "Configuration > Base de données."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

}
