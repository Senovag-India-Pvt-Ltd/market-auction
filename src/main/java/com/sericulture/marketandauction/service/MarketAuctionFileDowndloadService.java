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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MarketAuctionFileDowndloadService {

    @Autowired
    TransactionFileGenRepository transactionFileGenRepository;

    public ByteArrayInputStream generateCSV(int marketId,String fileName) {

        try {
            TransactionFileGeneration transactionFileGeneration = transactionFileGenRepository.getRowForCSV(marketId,fileName);
            Charset charset = StandardCharsets.UTF_8;
            byte[] byteArray = charset.encode(transactionFileGeneration.getObject()).array();
            return new ByteArrayInputStream(byteArray);
        } catch (Exception ex) {
            throw new RuntimeException("fail to import data to CSV file: " + ex.getMessage());

        }


    }
}
