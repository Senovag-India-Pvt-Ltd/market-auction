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

    // List-returning variant: a single fruitsId + lotNumber + numberOfDflsDisposed can have more than
    // one row, which makes the single-result finder above throw IncorrectResultSizeDataAccessException.
    // We match on numberOfDflsDisposed too so a disposal with a different DFL count is treated as a
    // different record (not offered as a candidate) — only true duplicates surface for selection.
    List<SaleAndDisposalOfDfls> findAllByFruitsIdAndLotNumberAndNumberOfDflsDisposedAndIsVerifiedAndActive(String fruitsId, String lotNumber,Long noOfDfls,Integer isVerified,boolean active);

    public SaleAndDisposalOfDfls findByFruitsIdAndIdAndActiveIn(@Param("fruitsId") String fruitsId,@Param("id") long id, @Param("active") Set<Boolean> active);

}
