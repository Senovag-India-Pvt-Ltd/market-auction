package com.sericulture.marketandauction.helper;


import com.sericulture.marketandauction.model.enums.LotStatus;

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

    public static final String ALLTTOTED_LOT_LIST_PER_MARKET_ID_AND_GODOWNID = ALLTTOTED_LOT_LIST_PER_MARKET_ID + " and ma.godown_id =:godownId ";

    private static final String SELECT_FIELDS_DTR_ONLINE = """
            select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.allotted_lot_id ,f.first_name,f.middle_name,f.last_name,f.farmer_number,
            f.mobile_number,l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,l.MARKET_FEE_REELER,
            r.reeling_license_number,r.name,r.mobile_number,
            fba.farmer_bank_name,fba.farmer_bank_branch_name ,fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number,mm.market_name_in_kannada,fa.address_text,l.auction_date """;

    private static final String FROM =" from ";

    private static final String SPACE = " ";


    private static final String LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_FARMER = """
             FARMER f
            INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID 
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date 
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date 
            LEFT JOIN dbo.farmer_address fa ON f.FARMER_ID = fa.FARMER_ID and fa.default_address = 1 
            LEFT JOIN  dbo.farmer_bank_account fba  ON   fba.FARMER_ID = f.FARMER_ID 
            INNER JOIN dbo.market_master mm on mm.market_master_id = ma.market_id 
            """;

    private static final String LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_REELER = """
              INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID  
             LEFT JOIN dbo.reeler_virtual_bank_account rvba ON rvba.reeler_id =r.reeler_id and rvba.market_master_id = ma.market_id
             LEFT JOIN dbo.REELER_VID_CURRENT_BALANCE rvcb ON rvcb.reeler_virtual_account_number= rvba.virtual_account_number """;

    private static final String SELECT_FIELDS_FARMER_TXN = """
             select  ROW_NUMBER() OVER(ORDER BY l.lot_id ASC) AS row_id,l.allotted_lot_id ,l.auction_date,
            f.first_name,f.middle_name,f.last_name,f.farmer_number,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,rm.race_name,v.VILLAGE_NAME """;

    private static final String WHERE_CLAUSE_DTR_ONLINE = """
              where l.status in ('readyforpayment','paymentsuccess','paymentfailed','paymentprocessing')
             and l.auction_date BETWEEN :fromDate and :toDate 
             and l.market_id =:marketId 
             and (:reelerIdList is null OR r.reeler_id in (:reelerIdList))
             and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 
             and rvcb.CURRENT_BALANCE > 0.0
            ORDER by l.lot_id""";

    private static final String FARMER_TXN_SPECIFIC_TABLES = """
             LEFT JOIN dbo.race_master rm on ma.RACE_MASTER_ID = rm.race_id
             LEFT JOIN  Village v ON   fa.Village_ID = v.village_id 
            """  ;

    private static final String WHERE_CLAUSE_FARMER_TXN = """
            where l.status in ('readyforpayment','paymentsuccess','paymentfailed','paymentprocessing')
             and l.auction_date BETWEEN :fromDate and :toDate 
             and l.market_id =:marketId and  f.farmer_number =:farmerNumber""";

    public static final String FARMER_TXN_REPORT = SELECT_FIELDS_FARMER_TXN +SPACE+FROM+SPACE+ LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_FARMER + SPACE+ FARMER_TXN_SPECIFIC_TABLES + SPACE + WHERE_CLAUSE_FARMER_TXN;


    public static final String DTR_ONLINE_REPORT_QUERY = SELECT_FIELDS_DTR_ONLINE + FROM + LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_FARMER +LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_REELER+ WHERE_CLAUSE_DTR_ONLINE;

    public static final String UNIT_COUNTER_REPORT_QUERY = """
            select  l.allotted_lot_id ,l.auction_date,
            l.LOT_WEIGHT_AFTER_WEIGHMENT,ra.AMOUNT,l.LOT_SOLD_OUT_AMOUNT ,l.MARKET_FEE_FARMER,l.MARKET_FEE_REELER,
            r.reeling_license_number,r.name
            from
            dbo.market_auction ma
            INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date
            INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID 
            INNER JOIN dbo.market_master mm on mm.market_master_id = ma.market_id
            where
            l.auction_date =:reportDate
            and l.market_id =:marketId
            ORDER by l.lot_id""";

    private static final String WHERE_CLAUSE_AUCTION_DATE_LIST = """
                 where l.status =:lotStatus
                         and l.market_id =:marketId
                         and fba.farmer_bank_account_number != ''
                         and fba.farmer_bank_ifsc_code !=''
                         and rvcb.CURRENT_BALANCE > 0.0
                        ORDER by l.auction_date""";

    public static final String AUCTION_DATE_LIST_BY_LOT_STATUS = "select  distinct l.auction_date " + FROM + SPACE + LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_FARMER + SPACE  +LOT_ACCEPTED_ALL_TABLES_FROM_CLAUSE_REELER+ SPACE + WHERE_CLAUSE_AUCTION_DATE_LIST;

    private static final String SELECT_FIELDS_PENDING_REPORT_BASE = """
            select  f.farmer_number,f.first_name ,f.middle_name,
             f.last_name,fa.address_text,t.TALUK_NAME,v.VILLAGE_NAME,
             fba.farmer_bank_ifsc_code ,fba.farmer_bank_account_number,
             l.allotted_lot_id,l.auction_date,ma.estimated_weight,
             mm.market_name,rm.race_name,sm.source_name,mm.box_weight,
             l.lot_id,mm.SERIAL_NUMBER_PREFIX,l.status,mm.market_name_in_kannada,
             f.name_kan,f.mobile_number,ma.market_auction_id,""";
    public static final String NEWLY_CREATED_LOTS = SELECT_FIELDS_PENDING_REPORT_BASE + """
             l.created_date
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
             WHERE l.auction_date =:paymentDate and l.market_id =:marketId and l.status is NULL""";

    public static final String NEWLY_CREATED_LOTS_FOR_PENDING_REPORT = SELECT_FIELDS_PENDING_REPORT_BASE + """
             l.created_date,
             gm.godown_name
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
             LEFT JOIN godown_master gm ON gm.godown_master_id = ma.godown_id  
             LEFT JOIN race_master rm ON rm.race_id = ma.RACE_MASTER_ID  
             LEFT JOIN source_master sm ON sm.source_id = ma.SOURCE_MASTER_ID 
             WHERE l.auction_date =:paymentDate and l.market_id =:marketId and l.status is NULL""";


    public static final String AND_LOT_ID = " and  l.allotted_lot_id =:allottedLotId";

    public static final String ACCEPTED_LOTS = SELECT_FIELDS_PENDING_REPORT_BASE + """
            ra.CREATED_DATE,
            r.reeling_license_number, r.name,
            r.address,l.LOT_WEIGHT_AFTER_WEIGHMENT,
            l.MARKET_FEE_REELER,l.MARKET_FEE_FARMER,l.LOT_SOLD_OUT_AMOUNT,
            ra.AMOUNT,rvcb.CURRENT_BALANCE,r.reeler_name_kannada,r.mobile_number,r.reeler_number,
            l.BID_ACCEPTED_BY
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
            WHERE l.auction_date =:paymentDate and l.market_id =:marketId
            """;
    public static final String ACTIVE_FILTERS_NEWLY_CREATED = " and f.ACTIVE =1 and ma.active = 1";

    public static final String ACTIVE_FILTERS_ACCEPTED_CREATED = " and r.active =1";

    public static final String PENDING_REPORT_NEWLY_CREATED_LOTS = NEWLY_CREATED_LOTS_FOR_PENDING_REPORT + " and (l.allotted_lot_id is null OR l.allotted_lot_id not in (:lotList))";

    public static final String PRINT_REPORT_NEWLY_CREATED_LOT_ID = NEWLY_CREATED_LOTS + SPACE +AND_LOT_ID +SPACE+ ACTIVE_FILTERS_NEWLY_CREATED;

    public static final String PRINT_REPORT_ACCEPTED_LOT_ID = ACCEPTED_LOTS + AND_LOT_ID + ACTIVE_FILTERS_NEWLY_CREATED+SPACE + ACTIVE_FILTERS_ACCEPTED_CREATED;

    public static final String LOT_CLAUSE_FOR_PENDING_REPORT =  " and ( l.status IS NULL OR l.status='accepted')";

    public static final String PENDING_REPORT_ACCEPTED_LOTS = ACCEPTED_LOTS + LOT_CLAUSE_FOR_PENDING_REPORT;

    private static final String BIDDING_REPORT_QUERY = """
            select l.allotted_lot_id ,r.reeling_license_number ,ra.AMOUNT ,ra.CREATED_DATE ,ra.STATUS ,ra.MODIFIED_DATE, l.BID_ACCEPTED_BY,mm.market_name 
            FROM dbo.lot l
            LEFT JOIN dbo.REELER_AUCTION ra ON ra.MARKET_ID  = l.market_id 
            and ra.ALLOTTED_LOT_ID  = l.allotted_lot_id  and ra.AUCTION_DATE =l.auction_date
            INNER JOIN dbo.reeler r ON r.reeler_id =ra.REELER_ID 
            INNER JOIN market_auction ma
            on ma.market_auction_id = l.market_auction_id 
            LEFT JOIN market_master mm ON mm.market_master_id = ma.market_id
            where l.auction_date =:auctionDate
            and l.market_id =:marketId
            
            """;

    public static final String BIDDING_REPORT_QUERY_LOT = BIDDING_REPORT_QUERY + "and l.allotted_lot_id =:lotId";

    public static final String BIDDING_REPORT_QUERY_REELER = BIDDING_REPORT_QUERY + "and r.reeling_license_number  =:reelerLicenseNumber";

}
