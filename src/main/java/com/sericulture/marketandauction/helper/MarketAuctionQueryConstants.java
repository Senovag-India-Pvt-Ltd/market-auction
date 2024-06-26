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
             l.created_date,
             f.fruits_id
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
            l.BID_ACCEPTED_BY, f.fruits_id
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

    public static final String DASHBOARD_COUNT = """
            SELECT\s
                             rm.race_name,
                             COUNT(ma.number_of_lot) AS total_lots,
                             SUM(lot.lot_sold_out_amount) AS total_sold_out_amount,
                             COUNT(lot.lot_id) AS total_lots_in_reeler_auction,
                             SUM(ra.amount) AS total_bids_amount,
                             COUNT(DISTINCT ra.reeler_id) AS unique_reeler_count,
                             COUNT(CASE WHEN lot.status = 'accepted' THEN 1 END) AS accepted_lots_count,
                             MAX(CASE WHEN lot.status = 'accepted' THEN ra.AMOUNT END) AS max_sold_out_amount_accepted,
                             MIN(CASE WHEN lot.status = 'accepted' THEN ra.AMOUNT END) AS min_sold_out_amount_accepted,
                             AVG(CASE WHEN lot.status = 'accepted' THEN ra.AMOUNT END) AS average_sold_out_amount_accepted,
                             SUM(lot.LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight_after_weighment
                             FROM\s
                                 market_auction ma
                             LEFT JOIN\s
                                 dbo.race_master rm ON ma.RACE_MASTER_ID = rm.race_id
                             LEFT JOIN\s
                                 lot ON ma.market_auction_id = lot.market_auction_id
                             LEFT JOIN\s
                                 reeler_auction ra ON lot.reeler_auction_id = ra.reeler_auction_id
                             WHERE\s
                                 ma.market_id = :marketId
                                 AND ma.market_auction_date = :marketAuctionDate
                             GROUP BY\s
                                 rm.race_name;""";

    public static final String ACCEPTANCE_STARTED = """
            SELECT\s
                 CASE\s
                     WHEN :marketAuctionDate BETWEEN AUCTION1_ACCEPT_START_TIME AND AUCTION1_ACCEPT_END_TIME OR :marketAuctionDate BETWEEN AUCTION2_ACCEPT_START_TIME AND AUCTION2_ACCEPT_END_TIME OR :marketAuctionDate BETWEEN AUCTION3_ACCEPT_START_TIME AND AUCTION3_ACCEPT_END_TIME THEN 'true'
                     ELSE 'false'
                 END AS is_currently_in_auction
             FROM\s
                 market_master\s
             WHERE\s
                 market_master_id = :marketId ;""";

    public static final String AUCTION_STARTED = """
            SELECT\s
                  CASE\s
                      WHEN :marketAuctionDate BETWEEN AUCTION_1_START_TIME AND AUCTION_1_END_TIME OR :marketAuctionDate BETWEEN AUCTION_2_START_TIME AND AUCTION_2_END_TIME OR :marketAuctionDate BETWEEN AUCTION_3_START_TIME AND AUCTION_3_END_TIME THEN 'true'
                      ELSE 'false'
                  END AS is_currently_in_auction
              FROM\s
                  market_master\s
              WHERE\s
                  market_master_id = :marketId ;""";

    public static final String GET_MARKET_NAME = """
            select market_name, market_name_in_kannada from market_master where market_master_id = :marketId ;""";

    public static final String PAYMENT_SUCCESS_LOTS = """
            select  COUNT(l.lot_id) from lot l\s
              INNER JOIN dbo.REELER_AUCTION ra ON ra.REELER_AUCTION_ID  = l.REELER_AUCTION_ID and ra.STATUS ='accepted' and ra.AUCTION_DATE =l.auction_date
              where l.status = 'paymentsucess' and l.market_id = :marketId and (:reelerIdList is null OR ra.reeler_id in (:reelerIdList))
              and l.auction_date BETWEEN :fromDate and :toDate ;""";

    public static final String REELER_PENDING_REPORT = """
            SELECT r.reeler_number, r.name, r.reeling_license_number, r.mobile_number, cm.CURRENT_BALANCE, cm.CREATED_DATE, cm.modified_date from reeler r
                          join user_master um on um.user_type_id = r.reeler_id
                          join REELER_VID_CURRENT_BALANCE cm on cm.REELER_ID = r.reeler_id
                          where um.market_id = :marketId ;""";

    public static final String break_down_of_lot_amount = """
            SELECT auction_date,
            market_id,
            SUM(LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
            SUM(LOT_SOLD_OUT_AMOUNT) AS total_amount,
            COUNT(lot_id) AS total_lot_count
            FROM lot
            WHERE rejected_by IS NULL
            AND LOT_SOLD_OUT_AMOUNT BETWEEN :fromAmount AND :toAmount
            AND market_id = :marketId
            AND auction_date = :auctionDate
            GROUP BY auction_date, market_id ;""";

    public static final String avg_of_lot_amount = """
            SELECT auction_date,
                   market_id,
                  \s
                   AVG(LOT_SOLD_OUT_AMOUNT) AS avg_amount
            FROM lot
            WHERE rejected_by IS NULL
            \s
              AND market_id = :marketId
              AND auction_date = :auctionDate
            GROUP BY auction_date, market_id ;""";

    public static final String greater_than_lot_amount = """
            SELECT auction_date,
                  market_id,
                  SUM(LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
               SUM(LOT_SOLD_OUT_AMOUNT) AS total_amount,
                  COUNT(lot_id) AS total_lot_count
           FROM lot
           WHERE rejected_by IS NULL
             AND LOT_SOLD_OUT_AMOUNT > :amount
             AND market_id = :marketId
             AND auction_date = :auctionDate
           GROUP BY auction_date, market_id
            ;""";

    public static final String less_than_lot_amount = """
            SELECT auction_date,
                  market_id,
                  SUM(LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
               SUM(LOT_SOLD_OUT_AMOUNT) AS total_amount,
                  COUNT(lot_id) AS total_lot_count
           FROM lot
           WHERE rejected_by IS NULL
             AND LOT_SOLD_OUT_AMOUNT < :amount
             AND market_id = :marketId
             AND auction_date = :auctionDate
           GROUP BY auction_date, market_id
            ;""";

    public static final String total_lot_status = """
            SELECT auction_date,
                   market_id,
                  \s
                   COUNT(LOT_ID) AS total_lots,
                   SUM(LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
                   SUM(LOT_SOLD_OUT_AMOUNT) AS total_amount,
                   MIN(LOT_SOLD_OUT_AMOUNT) AS min_amount,
                   MAX(LOT_SOLD_OUT_AMOUNT) AS max_amount,
                   AVG(LOT_SOLD_OUT_AMOUNT) AS avg_amount,
                   SUM(MARKET_FEE_REELER) as reeler_mf,
                   SUM(MARKET_FEE_FARMER) as farmer_mf
            FROM lot
            WHERE rejected_by IS NULL
            \s
              AND market_id = :marketId
              AND auction_date = :auctionDate
            GROUP BY auction_date, market_id ;""";

    public static final String state_wise_lot_status = """
            SELECT\s
                l.auction_date,
                l.market_id,
                COUNT(l.LOT_ID) AS total_lots,
                SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
                SUM(l.LOT_SOLD_OUT_AMOUNT) AS total_amount,
                MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_amount,
                MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_amount,
                AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_amount,
                SUM(l.MARKET_FEE_REELER) AS reeler_mf,
                SUM(l.MARKET_FEE_FARMER) AS farmer_mf,
                r.state_id,
                s.STATE_NAME
            FROM\s
                lot l
            JOIN\s
                REELER_AUCTION ra ON l.REELER_AUCTION_ID = ra.REELER_AUCTION_ID and ra.AUCTION_DATE = l.auction_date
            JOIN\s
                reeler r ON r.reeler_id = ra.REELER_ID and r.active = 1
            JOIN\s
                state s ON s.STATE_ID = r.state_id and s.ACTIVE = 1
            WHERE\s
                l.rejected_by IS NULL
                AND l.market_id = :marketId
                AND l.auction_date = :auctionDate
            GROUP BY\s
                l.auction_date, l.market_id, r.state_id, s.STATE_NAME ;""";

    public static final String race_wise_lot_status = """
            SELECT\s
                l.auction_date,
                l.market_id,
                COUNT(l.LOT_ID) AS total_lots,
                SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) AS total_weight,
                SUM(l.LOT_SOLD_OUT_AMOUNT) AS total_amount,
                MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_amount,
                MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_amount,
                AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_amount,
                SUM(l.MARKET_FEE_REELER) AS reeler_mf,
                SUM(l.MARKET_FEE_FARMER) AS farmer_mf,
                rm.race_id,
                rm.race_name
            FROM\s
                lot l
                JOIN market_auction ma on ma.market_auction_date = l.auction_date and ma.market_auction_id = l.market_auction_id
                JOIN race_master rm on ma.RACE_MASTER_ID = rm.race_id and rm.active = 1
            
            WHERE\s
                l.rejected_by IS NULL
                AND l.active = 1
                AND l.market_id = :marketId
                AND l.auction_date = :auctionDate
            GROUP BY\s
                l.auction_date, l.market_id,rm.race_id,rm.race_name ;""";

    public static final String reeler_report_for__app = """
            select l.allotted_lot_id,ra.AMOUNT, l.LOT_WEIGHT_AFTER_WEIGHMENT,l.LOT_SOLD_OUT_AMOUNT, l.MARKET_FEE_REELER
               from lot l
               join REELER_AUCTION ra on l.auction_date = ra.AUCTION_DATE and ra.REELER_AUCTION_ID = l.REELER_AUCTION_ID
               where l.auction_date = :auctionDate and l.market_id = :marketId and ra.REELER_ID = :reelerId ;""";

    public static final String reeler_current_balance = """
            select current_balance from REELER_VID_CURRENT_BALANCE where reeler_id = :reelerId ;""";

    public static final String reeler_deposited_amount = """
            SELECT SUM(amount) as total_amount_deposited FROM REELER_VID_CREDIT_TXN ct
            JOIN REELER_VID_CURRENT_BALANCE cb ON cb.reeler_virtual_account_number = ct.VIRTUAL_ACCOUNT
            WHERE cb.REELER_ID = :reelerId AND CAST(ct.TRANSACTION_DATE AS DATE) = :auctionDate ;""";

    public static final String reeler_purchase_amount = """
            SELECT SUM(amount) as total_purchase from REELER_AUCTION
               where reeler_id = :reelerId and market_id = :marketId and AUCTION_DATE = :auctionDate
               and STATUS in ('weighmentcompleted','readyforpayment','paymentsuccess','paymentfailed','paymentprocessing') ;""";

    public static final String reeler_auction_status = """
            SELECT status from REELER_AUCTION where REELER_AUCTION_ID = :reelerAuctionId ;""";

    public static final String total_reeler_balance = """
            SELECT SUM(cm.current_balance) as all_reeler_balance from reeler r
            join user_master um on um.user_type_id = r.reeler_id
            join REELER_VID_CURRENT_BALANCE cm on cm.REELER_ID = r.reeler_id
            where um.market_id = :marketId ;""";

    public static final String total_credit_txn_balance_today = """
            SELECT SUM(ct.amount) as total_amount_deposited FROM REELER_VID_CREDIT_TXN ct
              JOIN REELER_VID_CURRENT_BALANCE cb ON cb.reeler_virtual_account_number = ct.VIRTUAL_ACCOUNT
              join user_master um on um.user_type_id = cb.reeler_id
              WHERE um.market_id = :marketId AND CAST(ct.TRANSACTION_DATE AS DATE) = :auctionDate ;""";

    public static final String total_debit_txn_balance_today = """
            SELECT SUM(ct.amount) as total_amount_deposited FROM REELER_VID_DEBIT_TXN ct
             JOIN REELER_VID_CURRENT_BALANCE cb ON cb.reeler_virtual_account_number = ct.VIRTUAL_ACCOUNT
             join user_master um on um.user_type_id = cb.reeler_id
             WHERE um.market_id = :marketId AND CAST(ct.AUCTION_DATE AS DATE) = :auctionDate ;""";

    public static final String AVERAGE_REPORT_FOR_YEAR = """
            SELECT\s
                DATENAME(month, l.auction_date) AS month_name,
                AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_sold_amount,
                AVG(l.LOT_WEIGHT_AFTER_WEIGHMENT) AS avg_weight,
                rm.race_name
            FROM\s
                lot l
            JOIN\s
                market_auction ma ON ma.market_auction_id = l.market_auction_id
            JOIN\s
                race_master rm ON ma.RACE_MASTER_ID = rm.race_id
            WHERE
                l.auction_date >= :startDate AND l.auction_date < :endDate and rm.race_id = :raceId and ma.market_id = :marketId
            GROUP BY\s
                DATENAME(month, l.auction_date), rm.race_name
            ORDER BY\s
                DATENAME(month, l.auction_date) ;""";


    public static final String ACTIVE_RACE_FOR_AVERAGE_REPORT = """
            SELECT rm.race_id, rm.race_name
              FROM race_master rm
              JOIN market_auction ma ON rm.race_id = ma.race_master_id
              JOIN lot l ON l.market_auction_id = ma.market_auction_id
              
              WHERE\s
                  l.auction_date >= :startDate AND l.auction_date < :endDate and ma.market_id = :marketId
                GROUP BY rm.race_id, rm.race_name
                order by race_name asc ;""";

    public static final String AVERAGE_COCOON_REPORT = """
            SELECT YEAR(auction_date) AS auction_year,
            MONTH(auction_date) AS auction_month,
            AVG(LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS avg_lot_weight_in_tons,
            AVG(LOT_SOLD_OUT_AMOUNT) AS avg_sold_out_amount
             FROM lot
             where auction_date >= :startDate AND auction_date < :endDate and market_id = :marketId
             GROUP BY YEAR(auction_date), MONTH(auction_date)
             ORDER BY YEAR(auction_date), MONTH(auction_date) ;""";

    public static final String MARKETS_FOR_DTR_REPORT = """
            SELECT\s
                 l.market_id,\s
                 mm.market_name,
                 mm.market_name_in_kannada
             FROM\s
                 lot l
             JOIN\s
                 market_auction ma ON ma.market_auction_id = l.market_auction_id
             JOIN\s
                 market_master mm ON mm.market_master_id = l.market_id
             GROUP BY\s
                 l.market_id, mm.market_name, mm.market_name_in_kannada
             ORDER BY\s
                 mm.market_name ;""";

    public static final String RACES_BY_MARKET = """
            SELECT\s
               l.market_id,\s
            rm.race_id,
               rm.race_name,
               rm.race_name_in_kannada
           FROM\s
               lot l
           JOIN\s
               market_auction ma ON ma.market_auction_id = l.market_auction_id
           JOIN\s
               market_master mm ON mm.market_master_id = l.market_id
           JOIN\s
               race_master rm ON rm.race_id = ma.RACE_MASTER_ID
           where l.market_id = :marketId
           GROUP BY\s
               l.market_id, rm.race_name, rm.race_id,
               rm.race_name_in_kannada
           ORDER BY\s
               rm.race_name ;""";

    public static final String RACES_BY_NOT_MARKET = """
            SELECT\s
               l.market_id,\s
            rm.race_id,
               rm.race_name,
               rm.race_name_in_kannada
           FROM\s
               lot l
           JOIN\s
               market_auction ma ON ma.market_auction_id = l.market_auction_id
           JOIN\s
               market_master mm ON mm.market_master_id = l.market_id
           JOIN\s
               race_master rm ON rm.race_id = ma.RACE_MASTER_ID
           where l.market_id not in (:marketList)
           GROUP BY\s
               l.market_id, rm.race_name, rm.race_id,
               rm.race_name_in_kannada
           ORDER BY\s
               rm.race_name ;""";

    public static final String DTR_REPORT = """
            SELECT\s
              \s
                 MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_sold_out_amount,\s
                 MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_sold_out_amount,\s
                 AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_sold_out_amount,\s
                 SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton
             FROM\s
                 lot l
             JOIN\s
                 market_auction ma ON ma.market_auction_id = l.market_auction_id
             JOIN\s
                 market_master mm ON mm.market_master_id = l.market_id
             JOIN\s
                 race_master rm ON ma.RACE_MASTER_ID = rm.race_id
             where l.market_id = :marketId and rm.race_id = :raceId and l.auction_date = :auctionDate
             GROUP BY\s
                 l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String SUM_DTR_REPORT = """
            SELECT
               MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_sold_out_amount,
               MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_sold_out_amount,
               AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_sold_out_amount,
               SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton
               FROM
               lot l
               JOIN
               market_auction ma ON ma.market_auction_id = l.market_auction_id
                       JOIN
               market_master mm ON mm.market_master_id = l.market_id
                       JOIN
               race_master rm ON ma.RACE_MASTER_ID = rm.race_id
               where l.auction_date = :auctionDate ;""";

    public static final String SUM_DTR_REPORT_BY_RACE = """
            SELECT MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_sold_out_amount,
              MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_sold_out_amount,
              AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_sold_out_amount,
              SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton,
              rm.race_name_in_kannada
              FROM
              lot l
              JOIN
              market_auction ma ON ma.market_auction_id = l.market_auction_id
                      JOIN
              market_master mm ON mm.market_master_id = l.market_id
                      JOIN
              race_master rm ON ma.RACE_MASTER_ID = rm.race_id
              where rm.race_id = :raceId and l.auction_date = :auctionDate
              GROUP BY
              l.market_id, ma.RACE_MASTER_ID, rm.race_name_in_kannada ;""";

    public static final String GET_ALL_RACES = """
                select race_id, race_name, race_name_in_kannada from race_master where active = 1 ;""";

    public static final String TOTAL_BY_MONTH = """
            select SUM(LOT_WEIGHT_AFTER_WEIGHMENT)/1000 as total_weight_in_ton, SUM(LOT_SOLD_OUT_AMOUNT) /100000 as total_amount_in_lakh from lot
                where auction_date between :startDate and :endDate ;""";

    public static final String AUDIO_VISUAL_REPORT = """
            SELECT\s
              \s
                 MAX(l.LOT_SOLD_OUT_AMOUNT) AS max_sold_out_amount,\s
                 MIN(l.LOT_SOLD_OUT_AMOUNT) AS min_sold_out_amount,\s
                 AVG(l.LOT_SOLD_OUT_AMOUNT) AS avg_sold_out_amount,\s
                 SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton
             FROM\s
                 lot l
             JOIN\s
                 market_auction ma ON ma.market_auction_id = l.market_auction_id
             JOIN\s
                 market_master mm ON mm.market_master_id = l.market_id
             JOIN\s
                 race_master rm ON ma.RACE_MASTER_ID = rm.race_id
             where l.market_id = :marketId and rm.race_id = :raceId and l.auction_date between :startDate and :endDate
             GROUP BY\s
                 l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String MONTHLY_REPORT = """
            select rm.race_id, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as amount, SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT)/100 as weight, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as avg_amount,  rm.race_name_in_kannada from lot l
                join market_auction ma on ma.market_auction_date = l.auction_date and l.market_auction_id = ma.market_auction_id
                join race_master rm on rm.race_id = ma.RACE_MASTER_ID
                where l.auction_date between :startDate and :endDate
                group by rm.race_id, rm.race_name_in_kannada ;""";

    public static final String MONTHLY_REPORT_BY_RACE = """
            select rm.race_id, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as amount, SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT)/100 as weight, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as avg_amount,  rm.race_name_in_kannada from lot l
                join market_auction ma on ma.market_auction_date = l.auction_date and l.market_auction_id = ma.market_auction_id
                join race_master rm on rm.race_id = ma.RACE_MASTER_ID
                where l.auction_date between :startDate and :endDate and rm.race_id = :raceId
                group by rm.race_id, rm.race_name_in_kannada ;""";

    public static final String MONTHLY_REPORT_BY_STATE = """
            select  s.state_name_in_kannada, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as amount, SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT)/100 as weight, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as avg_amount from lot l
            join market_auction ma on ma.market_auction_date = l.auction_date and l.market_auction_id = ma.market_auction_id
            join race_master rm on rm.race_id = ma.RACE_MASTER_ID
            join farmer_address fa on fa.FARMER_ID = ma.farmer_id
            join state s on fa.STATE_ID = s.STATE_ID
            where l.auction_date between :startDate and :endDate and fa.STATE_ID = :stateId
            group by s.state_name_in_kannada ;""";

    public static final String GET_OTHER_STATES_DATA = """
            select  s.state_name_in_kannada, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as amount, SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT)/100 as weight, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 as avg_amount from lot l
            join market_auction ma on ma.market_auction_date = l.auction_date and l.market_auction_id = ma.market_auction_id
            join race_master rm on rm.race_id = ma.RACE_MASTER_ID
            join farmer_address fa on fa.FARMER_ID = ma.farmer_id
            join state s on fa.STATE_ID = s.STATE_ID
            where l.auction_date between :startDate and :endDate and fa.STATE_ID not in (:states)
            group by s.state_name_in_kannada ;""";

    public static final String GET_OVER_ALL_STATE_SUM = """
            SELECT s.state_name_in_kannada, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton FROM
            lot l
            JOIN
            market_auction ma ON ma.market_auction_id = l.market_auction_id
                    JOIN
            market_master mm ON mm.market_master_id = l.market_id
                    JOIN
            STATE s ON mm.state_id = s.STATE_ID
                    JOIN
            race_master rm ON ma.RACE_MASTER_ID = rm.race_id
            where  rm.race_id = :raceId and l.auction_date between :startDate and :endDate
            GROUP BY
            s.state_name_in_kannada, l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String GET_STATE_BY_STATE_NAME = """
            select * from STATE where STATE_NAME = :stateName ;""";

    public static final String MARKET_REPORT = """
            SELECT SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton FROM
                    lot l
                JOIN
                    market_auction ma ON ma.market_auction_id = l.market_auction_id
                JOIN
                    market_master mm ON mm.market_master_id = l.market_id
                JOIN
                    race_master rm ON ma.RACE_MASTER_ID = rm.race_id
                where l.market_id = :marketId and  rm.race_id = :raceId and l.auction_date between :startDate and :endDate
                GROUP BY
                    l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String MARKET_REPORT_BY_STATE_AND_RACE = """
            SELECT s.state_name_in_kannada, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton FROM
                  lot l
              JOIN
                  market_auction ma ON ma.market_auction_id = l.market_auction_id
              JOIN
                  market_master mm ON mm.market_master_id = l.market_id
            JOIN
              STATE s ON mm.state_id = s.STATE_ID
              JOIN
                  race_master rm ON ma.RACE_MASTER_ID = rm.race_id
              where s.STATE_ID = :stateId and  rm.race_id = :raceId and l.auction_date between :startDate and :endDate
              GROUP BY
                  s.state_name_in_kannada, l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String MARKET_REPORT_BY_STATE_AND_RACE_NOT_IN = """
            SELECT s.state_name_in_kannada, SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton FROM
                  lot l
              JOIN
                  market_auction ma ON ma.market_auction_id = l.market_auction_id
              JOIN
                  market_master mm ON mm.market_master_id = l.market_id
            JOIN
              STATE s ON mm.state_id = s.STATE_ID
              JOIN
                  race_master rm ON ma.RACE_MASTER_ID = rm.race_id
              where  s.STATE_ID not in (:states) and  rm.race_id = :raceId and l.auction_date between :startDate and :endDate
              GROUP BY
                  s.state_name_in_kannada, l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String DIVISION_WISE_SUM = """
            SELECT SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton FROM
                lot l
            JOIN
                market_auction ma ON ma.market_auction_id = l.market_auction_id
            JOIN
                market_master mm ON mm.market_master_id = l.market_id
            JOIN
                race_master rm ON ma.RACE_MASTER_ID = rm.race_id
            JOIN
                division_master dm ON mm.division_master_id = dm.division_master_id
            where dm.division_master_id= :divisionMasterId and rm.race_id = :raceId and l.auction_date between :startDate and :endDate
            GROUP BY
                l.market_id, ma.RACE_MASTER_ID ;""";

    public static final String MARKET_REPORT_SUM = """
            SELECT SUM(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS amount, AVG(l.LOT_SOLD_OUT_AMOUNT)/ 100000 AS avg_amount,
             SUM(l.LOT_WEIGHT_AFTER_WEIGHMENT) / 1000 AS sum_weight_after_weighment_in_ton, COUNT(l.lot_id) total_lots,
             SUM(l.market_fee_farmer + l.market_fee_reeler) / 100000 AS total_market_fee\s
             FROM
                              lot l
                          JOIN
                              market_auction ma ON ma.market_auction_id = l.market_auction_id
                          JOIN
                              market_master mm ON mm.market_master_id = l.market_id
                          JOIN
                              race_master rm ON ma.RACE_MASTER_ID = rm.race_id
                          where l.market_id = :marketId and l.auction_date between :startDate and :endDate
                          GROUP BY
                              l.market_id ;""";

    public static final String DISTRICT_BY_FARMER_ADDRESS = """
            select distinct(d.district_id), d.district_name_in_kannada from district d
             join farmer_address fa on d.DISTRICT_ID = fa.DISTRICT_ID ;""";

    public static final String VAHIVAATU_REPORT = """
            select SUM(LOT_WEIGHT_AFTER_WEIGHMENT) /1000 as sum_of_weight,fa.DISTRICT_ID, rm.race_name_in_kannada, d.district_name_in_kannada from lot l
             join market_auction ma on ma.market_auction_id = l.market_auction_id
             join farmer_address fa on fa.FARMER_ID = ma.farmer_id
             join race_master rm on rm.race_id = ma.RACE_MASTER_ID
             join DISTRICT d on d.DISTRICT_ID = fa.DISTRICT_ID
             where fa.DISTRICT_ID = :districtId and rm.race_id = :raceId and l.auction_date between :startDate and :endDate
             group by fa.DISTRICT_ID, rm.race_name_in_kannada, d.district_name_in_kannada ;""";

    public static final String VAHIVAATU_REPORT_TOTAL = """
            select SUM(LOT_WEIGHT_AFTER_WEIGHMENT) /1000 as sum_of_weight, rm.race_name_in_kannada from lot l
              join market_auction ma on ma.market_auction_id = l.market_auction_id
              join farmer_address fa on fa.FARMER_ID = ma.farmer_id
              join race_master rm on rm.race_id = ma.RACE_MASTER_ID
              join DISTRICT d on d.DISTRICT_ID = fa.DISTRICT_ID
              where  rm.race_id = :raceId and l.auction_date between :startDate and :endDate
              group by rm.race_name_in_kannada ;""";

    public static final String GET_ALL_DIVISIONS = """
            select division_master_id,name, name_in_kannada from  division_master where active = 1  ;""";

    public static final String GET_MARKET_BY_DIVISION = """
            select market_master_id, market_name, market_name_in_kannada from market_master where division_master_id = :divisionId  ;""";
}
