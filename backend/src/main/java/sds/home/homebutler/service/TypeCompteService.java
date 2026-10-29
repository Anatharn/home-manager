package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.TypeCompte;
import sds.home.homebutler.repository.TypeCompteRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TypeCompteService {

    private final TypeCompteRepository typeCompteRepository;

    public TypeCompteService(TypeCompteRepository typeCompteRepository) {
        this.typeCompteRepository = typeCompteRepository;
    }

    public List<TypeCompte> findAll() {
        return typeCompteRepository.findAll();
    }

    public Optional<TypeCompte> findById(Long id) {
        return typeCompteRepository.findById(id);
    }

    public TypeCompte save(TypeCompte typeCompte) {
        return typeCompteRepository.save(typeCompte);
    }

    public void deleteById(Long id) {
        typeCompteRepository.deleteById(id);
    }

}
