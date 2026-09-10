package sds.home.homebutler.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sds.home.homebutler.model.Transaction;
import sds.home.homebutler.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Paginé : évite d'envoyer tout l'historique des transactions au front. Tri par date
    // décroissante par défaut (les plus récentes d'abord), avec l'id en dernier recours pour
    // un ordre stable entre deux transactions à la même date.
    @GetMapping
    public PagedModel<Transaction> getAll(
            @RequestParam(required = false) Long compteId,
            @PageableDefault(size = 50, sort = {"dateOperation", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Transaction> page = compteId != null
                ? transactionService.findByCompteId(compteId, pageable)
                : transactionService.findAll(pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getById(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Transaction create(@RequestBody Transaction transaction) {
        return transactionService.save(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Transaction> update(@PathVariable Long id, @RequestBody Transaction transaction) {
        if (transactionService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        transaction.setId(id);
        return ResponseEntity.ok(transactionService.save(transaction));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transactionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
