package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.ReelerAuction;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface ReelerAuctionRepository  extends PagingAndSortingRepository<ReelerAuction, Integer> {

    public ReelerAuction save(ReelerAuction reelerAuction);

    @Query("select r from ReelerAuction r where r.allottedLotId =:lotId and" +
            " r.marketId =:marketId and r.auctionDate =:auctionDate order by amount desc,createdDate asc limit 1")
    public ReelerAuction getHighestBidForLot(int lotId,int marketId, LocalDate auctionDate);

    @Query(nativeQuery = true , value = "select  f.first_name,f.middle_name,f.last_name ,f.farmer_number ,r.name ,r.fruits_id,v.Village_Name,l.LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT \n" +
            " from  FARMER f ,reeler r,market_auction ma,lot l,farmer_address fa, Village v  " +
            " WHERE   l.market_auction_id =ma.market_auction_id and " +
            " ma.farmer_id = f.FARMER_ID and " +
            " l.allotted_lot_id =:lotId and " +
            " l.auction_date =:auctionDate and " +
            " l.market_id  =:marketId " +
            " and fa.FARMER_ID = f.FARMER_ID and fa.default_address = 1"+
            " and fa.Village_ID = v.village_id"+
            " and r.reeler_id =:reelerId")
    public List<Object[]> getLotBidDetailResponse(int lotId, LocalDate auctionDate, int marketId, BigInteger reelerId);

    public ReelerAuction findById(BigInteger id);
}
