package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.Compte;
import sds.home.homebutler.repository.CompteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CompteService {

    private final CompteRepository compteRepository;

    public CompteService(CompteRepository compteRepository) {
        this.compteRepository = compteRepository;
    }

    public List<Compte> findAll() {
        return compteRepository.findAll();
    }

    public Optional<Compte> findById(Long id) {
        return compteRepository.findById(id);
    }

    public Compte save(Compte compte) {
        return compteRepository.save(compte);
    }

    public void deleteById(Long id) {
        compteRepository.deleteById(id);
    }

}
