package sds.home.homebutler.service;

import org.springframework.stereotype.Service;

import sds.home.homebutler.model.FormatImportCsv;
import sds.home.homebutler.repository.FormatImportCsvRepository;

import java.util.List;
import java.util.Optional;

@Service
public class FormatImportCsvService {

    private final FormatImportCsvRepository formatImportCsvRepository;

    public FormatImportCsvService(FormatImportCsvRepository formatImportCsvRepository) {
        this.formatImportCsvRepository = formatImportCsvRepository;
    }

    public List<FormatImportCsv> findAll() {
        return formatImportCsvRepository.findAll();
    }

    public Optional<FormatImportCsv> findById(Long id) {
        return formatImportCsvRepository.findById(id);
    }

    public FormatImportCsv save(FormatImportCsv formatImportCsv) {
        return formatImportCsvRepository.save(formatImportCsv);
    }

    public void deleteById(Long id) {
        formatImportCsvRepository.deleteById(id);
    }

}
