package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** XC-004: rejected rows of legacy import runs. */
@Repository
public interface LegacyCocoonMarketImportErrorRepository extends JpaRepository<LegacyCocoonMarketImportError, Long> {

    List<LegacyCocoonMarketImportError> findTop1000ByLegacyCocoonMarketImportIdOrderByRowNumberInFileAsc(Long importId);
}
