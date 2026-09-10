package sds.home.homebutler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sds.home.homebutler.model.Banque;
import sds.home.homebutler.service.BanqueService;

import java.util.List;

@RestController
@RequestMapping("/api/banques")
public class BanqueController {

    private final BanqueService banqueService;

    public BanqueController(BanqueService banqueService) {
        this.banqueService = banqueService;
    }

    @GetMapping
    public List<Banque> getAll() {
        return banqueService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Banque> getById(@PathVariable Long id) {
        return banqueService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Banque create(@RequestBody Banque banque) {
        return banqueService.save(banque);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Banque> update(@PathVariable Long id, @RequestBody Banque banque) {
        if (banqueService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        banque.setId(id);
        return ResponseEntity.ok(banqueService.save(banque));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        banqueService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
