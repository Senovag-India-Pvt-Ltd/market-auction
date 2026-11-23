package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.LegacyCocoonMarketImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/** XC-004: import runs of legacy cocoon market exports. */
@Repository
public interface LegacyCocoonMarketImportRepository extends JpaRepository<LegacyCocoonMarketImport, Long> {

    List<LegacyCocoonMarketImport> findTop50ByActiveOrderByLegacyCocoonMarketImportIdDesc(Boolean active);
}
