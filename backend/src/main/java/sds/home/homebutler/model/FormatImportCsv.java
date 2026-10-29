package sds.home.homebutler.model;

import jakarta.persistence.*;

@Entity
@Table(name = "format_import_csv")
public class FormatImportCsv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    // JSON (délimiteur, séparateur décimal, format de date, mapping des colonnes...) : la
    // structure est définie et interprétée côté frontend (voir frontend/src/utils/csv.js et
    // formatsImportCsv.js), le backend ne fait que la stocker telle quelle.
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configuration;

    public FormatImportCsv() {
    }

    public FormatImportCsv(Long id, String nom, String configuration) {
        this.id = id;
        this.nom = nom;
        this.configuration = configuration;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

}
