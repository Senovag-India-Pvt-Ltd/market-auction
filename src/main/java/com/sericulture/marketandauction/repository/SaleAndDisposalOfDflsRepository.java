package com.sericulture.marketandauction.repository;


import com.sericulture.marketandauction.model.entity.SaleAndDisposalOfDfls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SaleAndDisposalOfDflsRepository extends JpaRepository<SaleAndDisposalOfDfls, Integer> {

    Optional<SaleAndDisposalOfDfls> findByIdAndActive(Integer id,boolean isActive);

    SaleAndDisposalOfDfls findByFruitsIdAndLotNumberAndIsVerifiedAndActive(String fruitsId, String lotNumber,Integer isVerified,boolean active);

    SaleAndDisposalOfDfls findByFruitsIdAndLotNumberAndNumberOfDflsDisposedAndIsVerifiedAndActive(String fruitsId, String lotNumber,Long noOfDfls,Integer isVerified,boolean active);

    // List-returning variant keyed on fruitsId + lotNumber only. A single fruitsId + lotNumber can
    // have more than one disposal row, which makes the single-result finders above throw
    // IncorrectResultSizeDataAccessException. We deliberately do NOT filter on numberOfDflsDisposed:
    // that column is the disposed-count, not the market-auction dfl lot number, so matching it
    // against dflLotNumber wrongly excluded valid rows (lookup returned 0 → disposal silently skipped).
    // isVerified + active keep us to live, verified disposals.
    List<SaleAndDisposalOfDfls> findAllByFruitsIdAndLotNumberAndIsVerifiedAndActive(String fruitsId, String lotNumber,Integer isVerified,boolean active);

    public SaleAndDisposalOfDfls findByFruitsIdAndIdAndActiveIn(@Param("fruitsId") String fruitsId,@Param("id") long id, @Param("active") Set<Boolean> active);

}
