package sds.home.homebutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "type_compte")
public class TypeCompte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    // JsonIgnore : évite la boucle infinie Compte -> TypeCompte -> comptes -> Compte -> ...
    // lors de la sérialisation JSON (relation bidirectionnelle).
    @JsonIgnore
    @OneToMany(mappedBy = "typeCompte", cascade = CascadeType.ALL)
    private List<Compte> comptes;

    public TypeCompte() {
    }

    public TypeCompte(Long id, String libelle, List<Compte> comptes) {
        this.id = id;
        this.libelle = libelle;
        this.comptes = comptes;
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

    public List<Compte> getComptes() {
        return comptes;
    }

    public void setComptes(List<Compte> comptes) {
        this.comptes = comptes;
    }

}
