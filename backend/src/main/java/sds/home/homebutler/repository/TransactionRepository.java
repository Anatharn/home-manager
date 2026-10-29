package sds.home.homebutler.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sds.home.homebutler.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByCompteId(Long compteId, Pageable pageable);

    @Query("SELECT t.compte.id, SUM(t.montant) FROM Transaction t GROUP BY t.compte.id")
    List<Object[]> sumMontantParCompte();

    @Query("SELECT COALESCE(SUM(t.montant), 0) FROM Transaction t WHERE t.compte.id = :compteId")
    BigDecimal sumMontantByCompteId(Long compteId);

}
