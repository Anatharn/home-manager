package sds.home.homebutler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sds.home.homebutler.model.Cheque;

import java.util.List;
import java.util.Optional;

public interface ChequeRepository extends JpaRepository<Cheque, Long> {

    List<Cheque> findByCompteId(Long compteId);

    Optional<Cheque> findByTransactionId(Long transactionId);

}
