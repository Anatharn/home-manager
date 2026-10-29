package sds.home.homebutler.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sds.home.homebutler.model.FormatImportCsv;
import sds.home.homebutler.service.FormatImportCsvService;

import java.util.List;

@RestController
@RequestMapping("/api/formats-import-csv")
public class FormatImportCsvController {

    private final FormatImportCsvService formatImportCsvService;

    public FormatImportCsvController(FormatImportCsvService formatImportCsvService) {
        this.formatImportCsvService = formatImportCsvService;
    }

    @GetMapping
    public List<FormatImportCsv> getAll() {
        return formatImportCsvService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormatImportCsv> getById(@PathVariable Long id) {
        return formatImportCsvService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public FormatImportCsv create(@RequestBody FormatImportCsv formatImportCsv) {
        return formatImportCsvService.save(formatImportCsv);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormatImportCsv> update(@PathVariable Long id, @RequestBody FormatImportCsv formatImportCsv) {
        if (formatImportCsvService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        formatImportCsv.setId(id);
        return ResponseEntity.ok(formatImportCsvService.save(formatImportCsv));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        formatImportCsvService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
