package sds.home.homebutler.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cheque")
public class Cheque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Column(name = "beneficiaire")
    private String beneficiaire;

    @Column(name = "montant", nullable = false, precision = 12, scale = 2)
    private BigDecimal montant;

    @Column(name = "date_emission", nullable = false)
    private LocalDate dateEmission;

    @Column(name = "date_encaissement")
    private LocalDate dateEncaissement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCheque statut;

    @ManyToOne
    @JoinColumn(name = "compte_id", nullable = false)
    private Compte compte;

    // Renseignée soit automatiquement à l'encaissement (ChequeService.encaisser), soit en
    // rattachant le chèque à une transaction déjà existante (ChequeService.lierTransaction).
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction transaction;

    // true si `transaction` a été créée par ce chèque (encaissement) : elle doit alors être
    // supprimée si le chèque est annulé/supprimé. false si elle a été liée manuellement à une
    // transaction déjà existante : dans ce cas on ne fait que délier, sans jamais la supprimer.
    @Column(name = "transaction_generee", nullable = false)
    private boolean transactionGeneree;

    public Cheque() {
    }

    public Cheque(Long id, String numero, String beneficiaire, BigDecimal montant, LocalDate dateEmission,
                  LocalDate dateEncaissement, StatutCheque statut, Compte compte, Transaction transaction,
                  boolean transactionGeneree) {
        this.id = id;
        this.numero = numero;
        this.beneficiaire = beneficiaire;
        this.montant = montant;
        this.dateEmission = dateEmission;
        this.dateEncaissement = dateEncaissement;
        this.statut = statut;
        this.compte = compte;
        this.transaction = transaction;
        this.transactionGeneree = transactionGeneree;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateEncaissement() {
        return dateEncaissement;
    }

    public void setDateEncaissement(LocalDate dateEncaissement) {
        this.dateEncaissement = dateEncaissement;
    }

    public StatutCheque getStatut() {
        return statut;
    }

    public void setStatut(StatutCheque statut) {
        this.statut = statut;
    }

    public Compte getCompte() {
        return compte;
    }

    public void setCompte(Compte compte) {
        this.compte = compte;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public boolean isTransactionGeneree() {
        return transactionGeneree;
    }

    public void setTransactionGeneree(boolean transactionGeneree) {
        this.transactionGeneree = transactionGeneree;
    }

}
