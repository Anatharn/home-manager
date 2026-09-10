package sds.home.homebutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "banque")
public class Banque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    // JsonIgnore : évite la boucle infinie Compte -> Banque -> comptes -> Compte -> ...
    // lors de la sérialisation JSON (relation bidirectionnelle).
    @JsonIgnore
    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL)
    private List<Compte> comptes;

    public Banque() {
    }

    public Banque(Long id, String nom, List<Compte> comptes) {
        this.id = id;
        this.nom = nom;
        this.comptes = comptes;
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

    public List<Compte> getComptes() {
        return comptes;
    }

    public void setComptes(List<Compte> comptes) {
        this.comptes = comptes;
    }

}
