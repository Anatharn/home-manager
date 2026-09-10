package sds.home.homebutler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sds.home.homebutler.model.Categorie;
import sds.home.homebutler.service.CategorieService;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategorieController {

    private final CategorieService categorieService;

    public CategorieController(CategorieService categorieService) {
        this.categorieService = categorieService;
    }

    @GetMapping
    public List<Categorie> getAll() {
        return categorieService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Categorie> getById(@PathVariable Long id) {
        return categorieService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Categorie create(@RequestBody Categorie categorie) {
        return categorieService.save(categorie);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Categorie> update(@PathVariable Long id, @RequestBody Categorie categorie) {
        if (categorieService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        categorie.setId(id);
        return ResponseEntity.ok(categorieService.save(categorie));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
