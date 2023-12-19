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

    @Query(nativeQuery = true , value = """
            select f.first_name,f.middle_name,f.last_name ,f.farmer_number ,v.Village_Name,l.LOT_APPROX_WEIGHT_BEFORE_WEIGHMENT,l.status,l.BID_ACCEPTED_BY  from\s
            FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID\s
            INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date\s
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1
            LEFT JOIN  Village v ON   fa.Village_ID=v.village_id where\s
            l.auction_date =:auctionDate and l.market_id =:marketId and l.allotted_lot_id =:lotId""")
    public Object[][] getLotBidDetailResponse(int lotId, LocalDate auctionDate, int marketId);

    public ReelerAuction findById(BigInteger id);

    @Query(nativeQuery = true, value = "SELECT r.name ,r.fruits_id  from REELER_AUCTION ra, reeler r  where ra.REELER_ID = r.reeler_id  and REELER_AUCTION_ID =:reelerAuctionId")
    public Object[][] getReelerDetailsForHighestBid(BigInteger reelerAuctionId);
}
