package sds.home.homebutler.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transaction")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_operation", nullable = false)
    private LocalDate dateOperation;

    @ManyToOne
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @Column(name = "commentaire", length = 2000)
    private String commentaire;

    public Transaction() {
    }

    public Transaction(Long id, String libelle, BigDecimal montant, LocalDate dateOperation, Compte compte,
                        Categorie categorie, String commentaire) {
        this.id = id;
        this.libelle = libelle;
        this.montant = montant;
        this.dateOperation = dateOperation;
        this.compte = compte;
        this.categorie = categorie;
        this.commentaire = commentaire;
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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public Compte getCompte() {
        return compte;
    }

    public void setCompte(Compte compte) {
        this.compte = compte;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

}
