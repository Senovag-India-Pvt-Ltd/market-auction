package com.sericulture.marketandauction.service;

import com.sericulture.marketandauction.model.entity.TransactionFileGeneration;
import com.sericulture.marketandauction.repository.TransactionFileGenRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

@Service
@Slf4j
public class MarketAuctionFileDowndloadService {

    @Autowired
    TransactionFileGenRepository transactionFileGenRepository;

    public ByteArrayInputStream generateCSV(String transactionFileGenId) {

        try {
            TransactionFileGeneration transactionFileGeneration = transactionFileGenRepository.getRowForCSV(transactionFileGenId);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), CSVFormat.DEFAULT);
            csvPrinter.printRecords(transactionFileGeneration.getObject());

            //c//svPrinter.printRecord(transactionFileGeneration.getObject());

            //csvPrinter.pr

            csvPrinter.flush();
           // ObjectOutputStream oos = new ObjectOutputStream(out);
            //oos.writeObject(transactionFileGeneration.getObject());
            //oos.flush();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException("fail to import data to CSV file: " + ex.getMessage());

        }


    }
}
