package com.sericulture.marketandauction.repository;


import com.sericulture.marketandauction.model.entity.SaleAndDisposalOfDfls;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface SaleAndDisposalOfDflsRepository extends JpaRepository<SaleAndDisposalOfDfls, Integer> {

    Optional<SaleAndDisposalOfDfls> findByIdAndActive(Integer id,boolean isActive);

    SaleAndDisposalOfDfls findByFruitsIdAndLotNumberAndActive(String fruitsId, String lotNumber,boolean active);

    public SaleAndDisposalOfDfls findByFruitsIdAndIdAndActiveIn(@Param("fruitsId") String fruitsId,@Param("id") long id, @Param("active") Set<Boolean> active);

}
