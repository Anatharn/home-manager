package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.Banque;
import sds.home.homebutler.repository.BanqueRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BanqueService {

    private final BanqueRepository banqueRepository;

    public BanqueService(BanqueRepository banqueRepository) {
        this.banqueRepository = banqueRepository;
    }

    public List<Banque> findAll() {
        return banqueRepository.findAll();
    }

    public Optional<Banque> findById(Long id) {
        return banqueRepository.findById(id);
    }

    public Banque save(Banque banque) {
        return banqueRepository.save(banque);
    }

    public void deleteById(Long id) {
        banqueRepository.deleteById(id);
    }

}
