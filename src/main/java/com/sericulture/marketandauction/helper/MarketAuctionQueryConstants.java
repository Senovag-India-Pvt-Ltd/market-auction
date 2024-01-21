package com.sericulture.marketandauction.helper;


public class MarketAuctionQueryConstants {

    public static final String getAllHighestBids = """
            SELECT ALLOTTED_LOT_ID,AMOUNT,R.Name  
            FROM REELER_AUCTION RAA INNER JOIN REELER R ON RAA.REELER_ID = R.REELER_ID 
            INNER JOIN (
            select MIN(REELER_AUCTION_ID) ID, RA.ALLOTTED_LOT_ID as AL from REELER_AUCTION RA,
            ( 
            SELECT MAX(AMOUNT) AMT, ALLOTTED_LOT_ID  from REELER_AUCTION ra
            where AUCTION_DATE = :today and ALLOTTED_LOT_ID in ( :lotList) AND MARKET_ID =:marketId GROUP by ALLOTTED_LOT_ID ) as RAB 
            WHERE RAB.AMT=RA.AMOUNT AND  RA.MARKET_ID =:marketId AND RA.ALLOTTED_LOT_ID = RAB.ALLOTTED_LOT_ID AND AUCTION_DATE = :today
            GROUP by  RA.ALLOTTED_LOT_ID ) RA ON RA.ID= RAA.REELER_AUCTION_ID
            """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID = """
            SELECT l.allotted_lot_id  from lot l
            INNER JOIN market_auction ma
            on ma.market_auction_id = l.market_auction_id
            where ma.market_auction_date =:auctionDate and ma.market_id = :marketId  and l.status is NULL """;

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID = ALLTTOTED_LOT_LIST_PER_MARKET_ID + " and ma.godown_id =:godownId";

    public static final String DTR_ONLINE_REPORT_QUERY = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.allotted_lot_id ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,l.MARKET_FEE_REELER,
            r.reeling_license_number,r.name,r.mobile_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number 
            from 
            dbo.FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
            LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
            LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID = f.FARMER_ID 
            LEFT JOIN dbo.market_master mm on mm.market_master_id = ma.market_auction_id 
            where l.status in ('readyforpayment','paymentsuccess','paymentfailed','paymentprocessing')
             and l.auction_date BETWEEN :fromDate and :toDate 
             and l.market_id =:marketId 
             and (:reelerIdList is null OR r.reeler_id in (:reelerIdList))
             and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 
             and rvcb.CURRENT_BALANCE > 0.0
            ORDER by l.lot_id""";

    public static final String UNIT_COUNTER_REPORT_QUERY = """
            select  l.allotted_lot_id ,l.auction_date,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,l.MARKET_FEE_REELER,
            r.reeling_license_number,r.name
            from
            dbo.market_auction ma
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID 
            LEFT JOIN dbo.market_master mm on mm.market_master_id = ma.market_auction_id
            where
            l.auction_date =:reportDate
            and l.market_id =:marketId
            ORDER by l.lot_id""";


}
