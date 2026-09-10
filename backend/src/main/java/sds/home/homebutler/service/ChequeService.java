package sds.home.homebutler.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sds.home.homebutler.model.Categorie;
import sds.home.homebutler.model.Cheque;
import sds.home.homebutler.model.StatutCheque;
import sds.home.homebutler.model.Transaction;
import sds.home.homebutler.repository.ChequeRepository;
import sds.home.homebutler.repository.TransactionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ChequeService {

    private final ChequeRepository chequeRepository;
    private final TransactionRepository transactionRepository;

    public ChequeService(ChequeRepository chequeRepository, TransactionRepository transactionRepository) {
        this.chequeRepository = chequeRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Cheque> findAll() {
        return chequeRepository.findAll();
    }

    public List<Cheque> findByCompteId(Long compteId) {
        return chequeRepository.findByCompteId(compteId);
    }

    public Optional<Cheque> findById(Long id) {
        return chequeRepository.findById(id);
    }

    public Cheque create(Cheque cheque) {
        cheque.setId(null);
        cheque.setStatut(StatutCheque.EMIS);
        cheque.setDateEncaissement(null);
        cheque.setTransaction(null);
        cheque.setTransactionGeneree(false);
        return chequeRepository.save(cheque);
    }

    /**
     * Modifie les informations d'un chèque. Refusé une fois le chèque encaissé ou annulé :
     * la transaction générée (ou l'absence d'impact) ne doit pas se retrouver désynchronisée
     * des informations du chèque.
     */
    public Cheque update(Long id, Cheque modifications) {
        Cheque existant = chequeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable"));
        if (existant.getStatut() != StatutCheque.EMIS) {
            throw new IllegalStateException("Un chèque encaissé ou annulé ne peut plus être modifié");
        }
        existant.setNumero(modifications.getNumero());
        existant.setBeneficiaire(modifications.getBeneficiaire());
        existant.setMontant(modifications.getMontant());
        existant.setDateEmission(modifications.getDateEmission());
        existant.setCompte(modifications.getCompte());
        return chequeRepository.save(existant);
    }

    /**
     * Encaisse un chèque émis : matérialise le débit en générant une Transaction liée
     * (montant négatif) sur le compte du chèque, à la date d'encaissement donnée.
     */
    @Transactional
    public Cheque encaisser(Long id, LocalDate dateEncaissement, Long categorieId) {
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable"));
        if (cheque.getStatut() != StatutCheque.EMIS) {
            throw new IllegalStateException("Seul un chèque émis peut être encaissé");
        }

        Transaction transaction = new Transaction();
        String libelle = "Chèque n°" + cheque.getNumero();
        if (cheque.getBeneficiaire() != null && !cheque.getBeneficiaire().isBlank()) {
            libelle += " — " + cheque.getBeneficiaire();
        }
        transaction.setLibelle(libelle);
        transaction.setMontant(cheque.getMontant().negate());
        transaction.setDateOperation(dateEncaissement);
        transaction.setCompte(cheque.getCompte());
        if (categorieId != null) {
            Categorie categorie = new Categorie();
            categorie.setId(categorieId);
            transaction.setCategorie(categorie);
        }
        Transaction transactionEnregistree = transactionRepository.save(transaction);

        cheque.setStatut(StatutCheque.ENCAISSE);
        cheque.setDateEncaissement(dateEncaissement);
        cheque.setTransaction(transactionEnregistree);
        cheque.setTransactionGeneree(true);
        return chequeRepository.save(cheque);
    }

    /**
     * Rattache un chèque émis à une transaction déjà existante sur le même compte (ex. importée
     * d'un relevé), au lieu d'en générer une nouvelle. La transaction n'est ni modifiée ni créée :
     * seul le lien est posé, et le chèque passe Encaissé à la date de cette transaction.
     */
    @Transactional
    public Cheque lierTransaction(Long chequeId, Long transactionId) {
        Cheque cheque = chequeRepository.findById(chequeId)
                .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable"));
        if (cheque.getStatut() != StatutCheque.EMIS) {
            throw new IllegalStateException("Seul un chèque émis peut être lié à une transaction");
        }
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction introuvable"));
        if (transaction.getCompte() == null || !transaction.getCompte().getId().equals(cheque.getCompte().getId())) {
            throw new IllegalStateException("La transaction doit appartenir au même compte que le chèque");
        }
        chequeRepository.findByTransactionId(transactionId).ifPresent(autre -> {
            throw new IllegalStateException("Cette transaction est déjà liée au chèque n°" + autre.getNumero());
        });

        cheque.setStatut(StatutCheque.ENCAISSE);
        cheque.setDateEncaissement(transaction.getDateOperation());
        cheque.setTransaction(transaction);
        cheque.setTransactionGeneree(false);
        return chequeRepository.save(cheque);
    }

    /**
     * Annule un chèque. S'il avait été encaissé, le lien vers la transaction est rompu ; la
     * transaction elle-même n'est supprimée que si elle avait été générée par ce chèque
     * (jamais si elle avait été liée à une transaction déjà existante). Idempotent si déjà annulé.
     */
    @Transactional
    public Cheque annuler(Long id) {
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chèque introuvable"));
        if (cheque.getStatut() == StatutCheque.ANNULE) {
            return cheque;
        }
        detacherTransaction(cheque);
        cheque.setStatut(StatutCheque.ANNULE);
        return chequeRepository.save(cheque);
    }

    @Transactional
    public void deleteById(Long id) {
        Cheque cheque = chequeRepository.findById(id).orElse(null);
        if (cheque == null) {
            return;
        }
        detacherTransaction(cheque);
        chequeRepository.deleteById(id);
    }

    private void detacherTransaction(Cheque cheque) {
        if (cheque.getTransaction() == null) {
            return;
        }
        if (cheque.isTransactionGeneree()) {
            transactionRepository.deleteById(cheque.getTransaction().getId());
        }
        cheque.setTransaction(null);
        cheque.setTransactionGeneree(false);
    }

}
