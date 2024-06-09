package com.sericulture.marketandauction.helper;

public class LotTransactionQueryConstants {

    public static final String ELIGIBLE_FOR_PAYEMNT_LOTS = """
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
            INNER JOIN dbo.market_master mm on mm.market_master_id = ma.market_id 
            where l.status =:lotStatus 
             and l.auction_date =:paymentDate
             and l.market_id =:marketId 
             and (:lotList is null OR l.allotted_lot_id in (:lotList))
            """;
    public static final String FOR_ONLINE_PAYMENT = """
            and fba.farmer_bank_account_number != '' 
             and fba.farmer_bank_ifsc_code !='' 
             and rvcb.CURRENT_BALANCE > 0.0 """;

    public static final String ORDER_BY_LOT_ID = " ORDER by l.lot_id";

    public static final String QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_ONLINE = ELIGIBLE_FOR_PAYEMNT_LOTS + FOR_ONLINE_PAYMENT + ORDER_BY_LOT_ID;

    public static final String QUERY_ELIGIBLE_FOR_PAYMENT_LOTS_CASH = ELIGIBLE_FOR_PAYEMNT_LOTS  + ORDER_BY_LOT_ID;
}
