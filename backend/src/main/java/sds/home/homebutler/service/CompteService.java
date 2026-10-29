package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.Compte;
import sds.home.homebutler.repository.CompteRepository;
import sds.home.homebutler.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompteService {

    private final CompteRepository compteRepository;
    private final TransactionRepository transactionRepository;

    public CompteService(CompteRepository compteRepository, TransactionRepository transactionRepository) {
        this.compteRepository = compteRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Compte> findAll() {
        List<Compte> comptes = compteRepository.findAll();
        Map<Long, BigDecimal> sommesParCompte = transactionRepository.sumMontantParCompte().stream()
                .collect(Collectors.toMap(ligne -> (Long) ligne[0], ligne -> (BigDecimal) ligne[1]));
        comptes.forEach(compte -> calculerSoldeActuel(compte, sommesParCompte.get(compte.getId())));
        return comptes;
    }

    public Optional<Compte> findById(Long id) {
        return compteRepository.findById(id)
                .map(compte -> {
                    calculerSoldeActuel(compte, transactionRepository.sumMontantByCompteId(id));
                    return compte;
                });
    }

    private void calculerSoldeActuel(Compte compte, BigDecimal sommeTransactions) {
        BigDecimal soldeInitial = compte.getSolde() != null ? compte.getSolde() : BigDecimal.ZERO;
        BigDecimal somme = sommeTransactions != null ? sommeTransactions : BigDecimal.ZERO;
        compte.setSoldeActuel(soldeInitial.add(somme));
    }

    public Compte save(Compte compte) {
        Compte saved = compteRepository.save(compte);
        BigDecimal sommeTransactions = saved.getId() != null
                ? transactionRepository.sumMontantByCompteId(saved.getId())
                : null;
        calculerSoldeActuel(saved, sommeTransactions);
        return saved;
    }

    public void deleteById(Long id) {
        compteRepository.deleteById(id);
    }

}
