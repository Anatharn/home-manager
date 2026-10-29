package sds.home.homebutler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sds.home.homebutler.model.TypeCompte;
import sds.home.homebutler.service.TypeCompteService;

import java.util.List;

@RestController
@RequestMapping("/api/types-compte")
public class TypeCompteController {

    private final TypeCompteService typeCompteService;

    public TypeCompteController(TypeCompteService typeCompteService) {
        this.typeCompteService = typeCompteService;
    }

    @GetMapping
    public List<TypeCompte> getAll() {
        return typeCompteService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TypeCompte> getById(@PathVariable Long id) {
        return typeCompteService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public TypeCompte create(@RequestBody TypeCompte typeCompte) {
        return typeCompteService.save(typeCompte);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TypeCompte> update(@PathVariable Long id, @RequestBody TypeCompte typeCompte) {
        if (typeCompteService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        typeCompte.setId(id);
        return ResponseEntity.ok(typeCompteService.save(typeCompte));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        typeCompteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
