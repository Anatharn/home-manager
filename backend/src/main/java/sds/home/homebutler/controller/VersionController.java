package sds.home.homebutler.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/version")
public class VersionController {

    public record VersionResponse(String version) {
    }

    @Value("${app.version}")
    private String version;

    @GetMapping
    public VersionResponse getVersion() {
        return new VersionResponse(version);
    }

}
