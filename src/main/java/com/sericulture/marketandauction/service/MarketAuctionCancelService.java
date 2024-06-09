package com.sericulture.marketandauction.service;

import com.sericulture.authentication.model.JwtPayloadData;
import com.sericulture.marketandauction.helper.MarketAuctionHelper;
import com.sericulture.marketandauction.helper.Util;
import com.sericulture.marketandauction.model.ResponseWrapper;
import com.sericulture.marketandauction.model.api.marketauction.CancelAuctionByLotRequest;
import com.sericulture.marketandauction.model.entity.Lot;
import com.sericulture.marketandauction.model.entity.ReelerAuction;
import com.sericulture.marketandauction.model.entity.ReelerVidDebitTxn;
import com.sericulture.marketandauction.model.enums.LotStatus;
import com.sericulture.marketandauction.repository.LotRepository;
import com.sericulture.marketandauction.repository.MarketMasterRepository;
import com.sericulture.marketandauction.repository.ReelerAuctionRepository;
import com.sericulture.marketandauction.repository.ReelerVidDebitTxnRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class MarketAuctionCancelService {

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private MarketAuctionHelper marketAuctionHelper;

    @Autowired
    ReelerAuctionRepository reelerAuctionRepository;

    @Autowired
    MarketMasterRepository marketMasterRepository;

    @Autowired
    ReelerVidDebitTxnRepository reelerVidDebitTxnRepository;


    public static List<String> eligibleStatusForCancellation = List.of(LotStatus.ACCEPTED.getLabel(), LotStatus.WEIGHMENTCOMPLETED.getLabel());

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ResponseEntity<?> cancelLot(CancelAuctionByLotRequest cancellationRequest) {
        ResponseWrapper rw = ResponseWrapper.createWrapper(List.class);
        try {
            JwtPayloadData token = marketAuctionHelper.getMOAuthToken(cancellationRequest);
            Lot lot = lotRepository.findByMarketIdAndAllottedLotIdAndAuctionDate(cancellationRequest.getMarketId(), cancellationRequest.getAllottedLotId(), cancellationRequest.getAuctionDate());
            if(lot.getStatus()!=null){
                if (lot.getStatus().equals(LotStatus.WEIGHMENTCOMPLETED.getLabel())) {
                    ReelerAuction reelerAuction = reelerAuctionRepository.findById(lot.getReelerAuctionId());
                    double lotSoldOutAmount = lot.getLotWeightAfterWeighment() * reelerAuction.getAmount();
                    Object[][] marketBrokarage = marketMasterRepository.getBrokarageInPercentageForMarket(lot.getMarketId());
                    double reelerBrokarage = Double.valueOf(String.valueOf(marketBrokarage[0][1]));
                    double reelerMarketFee = (lotSoldOutAmount * reelerBrokarage) / 100;
                    double amountDebitedFromReeler = Util.round(lotSoldOutAmount + reelerMarketFee, 2) * (-1);
                    String reelerVirtualBankAccount = reelerAuctionRepository.getReelerVirtualAccountByReelerIdAndMarketId(reelerAuction.getReelerId(), reelerAuction.getMarketId());
                    ReelerVidDebitTxn reelerVidDebitTxn = new ReelerVidDebitTxn(lot.getAllottedLotId(), lot.getMarketId(), Util.getISTLocalDate(), reelerAuction.getReelerId(), reelerVirtualBankAccount, amountDebitedFromReeler);
                    reelerVidDebitTxnRepository.save(reelerVidDebitTxn);
                }
                if (!eligibleStatusForCancellation.contains(lot.getStatus())) {
                    marketAuctionHelper.retrunIfError(rw, "Lot cannot be rejected as the status of lot is :" + lot.getStatus());
                }
            }
            lot.setStatus(LotStatus.CANCELLED.getLabel());
            lot.setReasonForCancellation(cancellationRequest.getCancellationReason());
            lot.setRejectedBy(token.getUsername());
            lotRepository.save(lot);
        } catch (Exception ex) {
            return marketAuctionHelper.retrunIfError(rw, "Error while cancelling lot : " + ex);
        }
        return ResponseEntity.ok(rw);

    }
}
