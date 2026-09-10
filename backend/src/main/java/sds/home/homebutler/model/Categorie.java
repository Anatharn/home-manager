package sds.home.homebutler.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categorie")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "couleur")
    private String couleur;

    public Categorie() {
    }

    public Categorie(Long id, String libelle, String couleur) {
        this.id = id;
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

}
