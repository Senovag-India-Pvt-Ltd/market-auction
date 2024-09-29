package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class MarketAuctionRequest extends RequestBody {

    @Schema(name = "farmerId", example = "123", required = true)
    @NotNull(message = "")
    private BigInteger farmerId;


    @Schema(name = "sourceMasterId", example = "2", required = true)
    private Integer sourceMasterId;


    @Schema(name = "raceMasterId", example = "1", required = true)
    private Integer raceMasterId;

    @Schema(name = "dflCount", example = "how many dfls", required = true)
    private int dflCount;


    @Schema(name = "estimatedWeight", example = "Estimated weight from farmer", required = true)
    private int estimatedWeight;

    @Schema(name = "numberOfLot", example = "number of lots assigned to this transaction", required = true)
    private int numberOfLot;


    @Schema(name = "numberOfSmallBin", example = "number of small bins assigned to this transaction", required = true)
    private int numberOfSmallBin;

    @Schema(name = "numberOfBigBin", example = "number of big bins assigned to this transaction", required = true)
    private int numberOfBigBin;

    private String dflLotNumber;
    private String lotVariety;
    private String lotParentalLevel;


    @Override
    public List<ValidationMessage> validate(){
        List<ValidationMessage> validationMessageList = new ArrayList<ValidationMessage>();

        return validationMessageList;
    }
}
