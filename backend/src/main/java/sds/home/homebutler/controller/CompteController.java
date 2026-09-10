package sds.home.homebutler.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sds.home.homebutler.model.Compte;
import sds.home.homebutler.service.CompteService;

import java.util.List;

@RestController
@RequestMapping("/api/comptes")
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
        this.compteService = compteService;
    }

    @GetMapping
    public List<Compte> getAll() {
        return compteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compte> getById(@PathVariable Long id) {
        return compteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Compte create(@Valid @RequestBody Compte compte) {
        return compteService.save(compte);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compte> update(@PathVariable Long id, @Valid @RequestBody Compte compte) {
        if (compteService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        compte.setId(id);
        return ResponseEntity.ok(compteService.save(compte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        compteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
