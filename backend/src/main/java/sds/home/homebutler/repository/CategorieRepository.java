package sds.home.homebutler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sds.home.homebutler.model.Categorie;

public interface CategorieRepository extends JpaRepository<Categorie, Long> {
}
