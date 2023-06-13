package com.ncc.kairos.moirai.zeus.dao;

import com.ncc.kairos.moirai.zeus.model.StoredFile;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface StoredFileRepository extends CrudRepository<StoredFile, String> {
    List<StoredFile> findAll();

    List<StoredFile> findAllByOwner(String owner);

    List<StoredFile> findAllByOwnerAndId(String owner, String id);

    List<StoredFile> findAllByOwnerAndExperiment(String owner, String experiment);

    List<StoredFile> findAllByPublicAccessAndExperiment(Boolean publicAccess, String experiment);

    List<StoredFile> findAllByUri(String uri);

    List<StoredFile> findAllByExperiment(String experiment);

    void deleteById(String id);
}
