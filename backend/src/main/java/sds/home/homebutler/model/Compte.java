package sds.home.homebutler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "compte")
public class Compte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String libelle;

    @Column(name = "iban")
    private String iban;

    @NotBlank(message = "Le code du compte est obligatoire")
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "solde", precision = 12, scale = 2)
    private BigDecimal solde;

    @ManyToOne
    @JoinColumn(name = "banque_id")
    private Banque banque;

    // JsonIgnore : évite la boucle infinie Transaction -> Compte -> transactions -> Compte -> ...
    // lors de la sérialisation JSON (relation bidirectionnelle).
    @JsonIgnore
    @OneToMany(mappedBy = "compte", cascade = CascadeType.ALL)
    private List<Transaction> transactions;

    // Idem : évite Cheque -> Compte -> cheques -> Cheque -> ..., et le cascade=ALL permet
    // de supprimer un compte sans violer la contrainte de clé étrangère de cheque.compte_id.
    @JsonIgnore
    @OneToMany(mappedBy = "compte", cascade = CascadeType.ALL)
    private List<Cheque> cheques;

    public Compte() {
    }

    public Compte(Long id, String libelle, String iban, String code, BigDecimal solde, Banque banque,
                  List<Transaction> transactions, List<Cheque> cheques) {
        this.id = id;
        this.libelle = libelle;
        this.iban = iban;
        this.code = code;
        this.solde = solde;
        this.banque = banque;
        this.transactions = transactions;
        this.cheques = cheques;
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getSolde() {
        return solde;
    }

    public void setSolde(BigDecimal solde) {
        this.solde = solde;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public List<Cheque> getCheques() {
        return cheques;
    }

    public void setCheques(List<Cheque> cheques) {
        this.cheques = cheques;
    }

}
