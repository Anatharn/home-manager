package sds.home.homebutler.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import sds.home.homebutler.dto.DatabaseConnexionConfig;
import sds.home.homebutler.service.DatabaseConfigService;

@RestController
@RequestMapping("/api/configuration/database")
public class DatabaseConfigController {

    private final DatabaseConfigService databaseConfigService;

    public DatabaseConfigController(DatabaseConfigService databaseConfigService) {
        this.databaseConfigService = databaseConfigService;
    }

    @GetMapping
    public DatabaseConnexionConfig getConfig() {
        return databaseConfigService.lire();
    }

    @PutMapping
    public DatabaseConnexionConfig updateConfig(@RequestBody DatabaseConnexionConfig config) {
        try {
            return databaseConfigService.enregistrer(config);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

}
