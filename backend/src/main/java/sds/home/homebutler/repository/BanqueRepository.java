package sds.home.homebutler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sds.home.homebutler.model.Banque;

public interface BanqueRepository extends JpaRepository<Banque, Long> {
}
