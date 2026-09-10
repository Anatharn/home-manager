package sds.home.homebutler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import sds.home.homebutler.model.Cheque;
import sds.home.homebutler.service.ChequeService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cheques")
public class ChequeController {

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    public record EncaisserRequest(LocalDate dateEncaissement, Long categorieId) {
    }

    public record LierTransactionRequest(Long transactionId) {
    }

    @GetMapping
    public List<Cheque> getAll(@RequestParam(required = false) Long compteId) {
        if (compteId != null) {
            return chequeService.findByCompteId(compteId);
        }
        return chequeService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cheque> getById(@PathVariable Long id) {
        return chequeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Cheque create(@RequestBody Cheque cheque) {
        return chequeService.create(cheque);
    }

    @PutMapping("/{id}")
    public Cheque update(@PathVariable Long id, @RequestBody Cheque cheque) {
        try {
            return chequeService.update(id, cheque);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/encaisser")
    public Cheque encaisser(@PathVariable Long id, @RequestBody EncaisserRequest requete) {
        try {
            LocalDate date = requete.dateEncaissement() != null ? requete.dateEncaissement() : LocalDate.now();
            return chequeService.encaisser(id, date, requete.categorieId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/lier-transaction")
    public Cheque lierTransaction(@PathVariable Long id, @RequestBody LierTransactionRequest requete) {
        try {
            return chequeService.lierTransaction(id, requete.transactionId());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }

    @PostMapping("/{id}/annuler")
    public Cheque annuler(@PathVariable Long id) {
        try {
            return chequeService.annuler(id);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        chequeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
