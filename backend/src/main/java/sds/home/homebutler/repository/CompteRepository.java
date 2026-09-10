package sds.home.homebutler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sds.home.homebutler.model.Compte;

public interface CompteRepository extends JpaRepository<Compte, Long> {
}
