package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.Categorie;
import sds.home.homebutler.repository.CategorieRepository;

import java.util.List;
import java.util.Optional;

@Service
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public CategorieService(CategorieRepository categorieRepository) {
        this.categorieRepository = categorieRepository;
    }

    public List<Categorie> findAll() {
        return categorieRepository.findAll();
    }

    public Optional<Categorie> findById(Long id) {
        return categorieRepository.findById(id);
    }

    public Categorie save(Categorie categorie) {
        return categorieRepository.save(categorie);
    }

    public void deleteById(Long id) {
        categorieRepository.deleteById(id);
    }

}
