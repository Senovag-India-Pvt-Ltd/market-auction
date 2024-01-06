package com.sericulture.marketandauction.repository;

import com.sericulture.marketandauction.model.entity.Lot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;

public interface LotRepository extends PagingAndSortingRepository<Lot, BigInteger> {

    public Lot save(Lot lot);

    public Iterable<Lot> saveAll(Iterable<Lot> lotList);

    @Query("select max(l.allottedLotId) from Lot l where l.marketId=:marketId and l.auctionDate=:auctionDate")
    public Integer findByMarketIdAndAuctionDate(@Param("marketId") int marketId, @Param("auctionDate") LocalDate auctionDate);

    /*
    @Query("select max(l.allottedLotId) from Lot l where l.marketId=:marketId and l.godownId =:godownId and l.auctionDate=:auctionDate")
    public Integer findByMarketIdAndAuctionDate(@Param("marketId") int marketId, @Param("auctionDate") LocalDate auctionDate);
*/
    @Query("select l.allottedLotId from Lot l where l.marketAuctionId=:marketAuctionId")
    public List<Integer> findAllAllottedLotsByMarketAuctionId(@Param("marketAuctionId") BigInteger marketAuctionId);

    public List<Lot> findAllByMarketAuctionId(BigInteger marketAuctionId);

    public Lot findByMarketIdAndAllottedLotIdAndAuctionDate(int marketId, int allottedLotId, LocalDate auctionDate);

    @Query(nativeQuery = true, value = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.lot_id as FARMER_PAYMENT_ID ,l.allotted_lot_id,l.auction_date ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,r.reeling_license_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number ,
            l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER 
            ,rvcb.CURRENT_BALANCE,
            rvba.virtual_account_number
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID  = f.FARMER_ID 
            LEFT JOIN dbo.market_master mm on mm.market_master_id = ma.market_auction_id 
            LEFT JOIN dbo.market_type_master mtm ON mtm.market_type_master_id = mm.market_master_id 
             where l.status ='weighmentcompleted' 
             and l.market_id =:marketId 
            ORDER by l.lot_id""")
    public Page<Object[]> getAllWeighmentCompletedTxnByMarket(final Pageable pageable, int marketId);


    @Query(nativeQuery = true, value = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.lot_id as FARMER_PAYMENT_ID ,l.allotted_lot_id,l.auction_date ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,r.reeling_license_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number ,
            l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER 
            ,rvcb.CURRENT_BALANCE,
            rvba.virtual_account_number
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID  = f.FARMER_ID 
            LEFT JOIN dbo.market_master mm on mm.market_master_id = ma.market_auction_id 
            where l.status =:lotStatus 
             and l.auction_date =:paymentDate
             and l.market_id =:marketId 
             and (:lotList is null OR l.allotted_lot_id in (:lotList))
             and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 
             and rvcb.CURRENT_BALANCE > 0.0
            ORDER by l.lot_id""")
    public List<Object[]> getAllEligiblePaymentTxnByOptionalLotListAndLotStatus(LocalDate paymentDate, int marketId, List<Integer> lotList,String lotStatus);



    @Query(nativeQuery = true, value = """
            select  f.farmer_number,f.first_name ,f.middle_name,f.last_name,fa.address_text,t.TALUK_NAME,v.VILLAGE_NAME,
            fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number,
            l.allotted_lot_id,l.auction_date,ma.estimated_weight,
            mm.market_name,rm.race_name,sm.source_name,mm.box_weight,
            l.lot_id,mm.SERIAL_NUMBER_PREFIX,
            r.reeling_license_number, r.name,
            r.address,l.LOT_WEIGHT_AFTER_WEIGHMENT,
            l.MARKET_FEE_REELER,l.MARKET_FEE_FARMER,l.LOT_SOLD_OUT_AMOUNT,
            ra.AMOUNT,rvcb.CURRENT_BALANCE
            from 
            FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id  
            INNER JOIN REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID
            INNER JOIN reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  Village v ON   fa.Village_ID = v.village_id 
            LEFT JOIN farmer_bank_account fba ON fba.FARMER_ID = f.FARMER_ID 
            LEFT JOIN TALUK t on t.TALUK_ID = fa.TALUK_ID
            LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id  
            LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID  
            LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID  
            WHERE l.auction_date =:paymentDate and l.market_id =:marketId and  l.allotted_lot_id =:allottedLotId 
            and f.ACTIVE =1 and ma.active = 1 and r.active =1""")
    public Object[][] getAcceptedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);

    
    @Query(nativeQuery = true,value = """
            select  f.farmer_number,f.first_name ,f.middle_name,
            f.last_name,fa.address_text,t.TALUK_NAME,v.VILLAGE_NAME,
            fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number,
            l.allotted_lot_id,l.auction_date,ma.estimated_weight,
             mm.market_name,rm.race_name,sm.source_name,mm.box_weight,
             l.lot_id,mm.SERIAL_NUMBER_PREFIX
            from  
            FARMER f
            INNER JOIN market_auction ma ON ma.farmer_id = f.FARMER_ID  
            INNER JOIN lot l ON l.market_auction_id =ma.market_auction_id  
            and l.auction_date = ma.market_auction_date  
            LEFT JOIN farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1  
            LEFT JOIN  Village v ON   fa.Village_ID = v.village_id  
            LEFT JOIN farmer_bank_account fba ON fba.FARMER_ID = f.FARMER_ID  
            LEFT JOIN TALUK t on t.TALUK_ID = fa.TALUK_ID
            LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id  
            LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID  
            LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID 
            WHERE l.auction_date =:paymentDate and l.market_id =:marketId and  l.allotted_lot_id =:allottedLotId""")
    public Object[][] getNewlyCreatedLotDetails(LocalDate paymentDate,int marketId,int allottedLotId);


    @Query("select distinct auctionDate from Lot where status=:status and marketId=:marketId")
    public List<LocalDate> getAllWeighmentCompletedOrReadyForPaymentAuctionDatesByMarket(int marketId,String status);

}
