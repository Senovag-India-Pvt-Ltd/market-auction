package com.sericulture.marketandauction.service;


import com.sericulture.marketandauction.model.api.ReelerTransactionReport;
import com.sericulture.marketandauction.model.api.ReelerTransactionReportWrapper;
import com.sericulture.marketandauction.model.entity.ReportAllTransaction;
import com.sericulture.marketandauction.model.entity.ReportCurrentBalance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ReportService {

    private String REELER_QUERY = """
                    SELECT
                        name,
                        virtual_account_number
                    FROM
                        reeler r
                    INNER JOIN
                        reeler_virtual_bank_account rvba
                        ON rvba.reeler_id = r.reeler_id
                    WHERE
                        rvba.market_master_id =:marketId
                        AND r.reeling_license_number = :reelerNumber
                   """;

    private String CURRENT_BALANCE_QUERY = """
            SELECT
                CURRENT_BALANCE,
                reeler_virtual_account_number,
                CREATED_DATE
            FROM
                REELER_VID_CURRENT_BALANCE
            WHERE
                reeler_virtual_account_number =:virtualAccount
            """;
    private String TRANSACTION_PASS_BOOK = """
            SELECT I.TXN_TYPE,AMOUNT, CREATED_DATE, LOT_ID, CAST(I.CREATED_DATE AS DATE) DATE_ON,FARMER_NAME
            FROM (
                SELECT
                    'C' AS TXN_TYPE,
                    AMOUNT,
                    CREATED_DATE,
                    -1 as LOT_ID,
                    VIRTUAL_ACCOUNT,
                    '-' AS FARMER_NAME
                FROM REELER_VID_CREDIT_TXN
                WHERE
                    CREATED_DATE BETWEEN :fromDate AND :toDate
                    AND VIRTUAL_ACCOUNT = :vAccount
            UNION ALL
                SELECT
                    'D' AS TXN_TYPE,
                     AMOUNT,
                     CREATED_DATE,
                     LOT_ID,
                     VIRTUAL_ACCOUNT,
                     (SELECT
                          f.first_name
                      FROM
                      dbo.FARMER f
                                 INNER JOIN dbo.market_auction ma ON ma.farmer_id = f.FARMER_ID
                                 INNER JOIN dbo.lot l ON l.market_auction_id =ma.market_auction_id and l.auction_date = ma.market_auction_date
                                 WHERE l.allotted_lot_id =DT.LOT_ID  AND l.auction_date =DT.AUCTION_DATE  AND l.market_id =:marketId
                      ) AS FARMER_NAME
                FROM REELER_VID_DEBIT_TXN DT
                WHERE CREATED_DATE BETWEEN :fromDate AND :toDate
                AND VIRTUAL_ACCOUNT = :vAccount
            ) as I
            WHERE I.VIRTUAL_ACCOUNT = :vAccount
            ORDER BY I.CREATED_DATE ASC
            """;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    public ReelerTransactionReportWrapper generateReelerReport(int marketId, String reelerNumber, LocalDate fromDate, LocalDate toDate){

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        Object[] reelerResult =  (Object[])entityManager.createNativeQuery(REELER_QUERY)
                .setParameter("marketId", marketId)
                .setParameter("reelerNumber", reelerNumber)
                .getSingleResult();

        if(Objects.isNull(reelerResult)){
            return new ReelerTransactionReportWrapper();
        }
        String reelerName =(String)reelerResult[0];
        String virtualAccountNumber = (String) reelerResult[1];

        //Balance Query
        Object object = entityManager.createNativeQuery(CURRENT_BALANCE_QUERY)
                .setParameter("virtualAccount", virtualAccountNumber)
                .getSingleResult();
        Object[] array = (Object[])object;
        ReportCurrentBalance reportCurrentBalance = new ReportCurrentBalance();
        reportCurrentBalance.setCurrentBalance(((BigDecimal)array[0]).doubleValue());
        reportCurrentBalance.setVirtualAccountNumber((String)array[1]);
        reportCurrentBalance.setCreatedDate(((Timestamp) array[2]).toLocalDateTime());

        List<Object[]> objectList = entityManager.createNativeQuery(TRANSACTION_PASS_BOOK)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
               // .setParameter("balanceTime", Timestamp.valueOf(reportCurrentBalance.getCreatedDate()))
                .setParameter("vAccount", reportCurrentBalance.getVirtualAccountNumber())
                .setParameter("marketId",marketId)
                .getResultList();

        List<ReportAllTransaction> reportAllTransactions = new ArrayList<>();
        for(Object[] obj : objectList){
            ReportAllTransaction rat = new ReportAllTransaction();
            rat.setTransactionType(obj[0]+"");
            rat.setAmount(((BigDecimal) obj[1]).doubleValue());
            rat.setCreatedDate(((Timestamp) obj[2]).toLocalDateTime());
            rat.setLot((Long) obj[3]);
            rat.setDateOn(((java.sql.Date) obj[4]).toLocalDate());
            rat.setFarmerName((String)obj[5]);
            reportAllTransactions.add(rat);
        }
        double debitSum = 0.00;
        double creditSum= 0.00;
        double creditDebitDiff = 0.00;
        double openingBalance = 0.00;
        double currentBalance = 0.00;

        List<ReelerTransactionReport> reelerTransactionReports = new ArrayList<>();
        // prepare all the sums
        for(ReportAllTransaction rat: reportAllTransactions)  {
            ReelerTransactionReport rtp = new ReelerTransactionReport();
            if(Objects.isNull(rat.getAmount())){
                continue;
            }
            rtp.setTransactionType(rat.getTransactionType());
            rtp.setTransactionDate(rat.getCreatedDate().toLocalDate());
            if("D".equalsIgnoreCase(rat.getTransactionType())){
                debitSum = debitSum + rat.getAmount();
                rtp.setPaymentAmount(rat.getAmount());
                rtp.setOperationDescription("Paid to "+rat.getFarmerName()+", for lot "+ rat.getLot());
            } else {
                creditSum = creditSum +rat.getAmount();
                rtp.setDepositAmount(rat.getAmount());
                rtp.setOperationDescription("Deposited by "+ reelerName);
            }
            reelerTransactionReports.add(rtp);
        }
        //susbtract credit and debit
        creditDebitDiff = debitSum - creditSum;
        openingBalance = currentBalance - creditDebitDiff;

        double runningBalance = openingBalance;
        //Need to loop again to fill the running balance
        for (ReelerTransactionReport rtp: reelerTransactionReports){
            if("D".equalsIgnoreCase(rtp.getTransactionType())){
                runningBalance = runningBalance - rtp.getPaymentAmount();
                rtp.setBalance(runningBalance);
            } else {
                runningBalance =runningBalance+rtp.getDepositAmount();
                rtp.setBalance(runningBalance);
            }
        }
        ReelerTransactionReportWrapper reelerTransactionReportWrapper = new ReelerTransactionReportWrapper();
        reelerTransactionReportWrapper.setReelerTransactionReports(reelerTransactionReports);
        reelerTransactionReportWrapper.setOpeningBalance(openingBalance);
        reelerTransactionReportWrapper.setTotalDeposits(creditSum);
        reelerTransactionReportWrapper.setTotalPurchase(debitSum);
        reelerTransactionReportWrapper.setName(reelerName);
        return reelerTransactionReportWrapper;
    }
}
