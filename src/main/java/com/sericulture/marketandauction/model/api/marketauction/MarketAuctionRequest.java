package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;
import com.sericulture.marketandauction.model.exceptions.ValidationMessage;
import com.sericulture.marketandauction.validators.PresentDate;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class MarketAuctionRequest extends RequestBody {

    @Schema(name = "marketId", example = "1", required = true)
    //@Size(min = 1, max = 2 , message = "MA00202.LOT.INVALID_MARKET_ID_SIZE")
    private int marketId;

    @Schema(name = "gowdownId", example = "1", required = false)
    //@Size(min = 1, max = 2 , message = "MA00202.LOT.INVALID_GODOWN_ID_SIZE")
    private int godownId;

    @Schema(name = "farmerId", example = "123", required = true)
    //@Size(min = 1, max = 15 , message = "MA00202.LOT.INVALID_FARMER_ID_SIZE")
    @NotNull(message = "")
    private BigInteger farmerId;

    @NotNull(message = "MA00202.LOT.NOT_NULL")
    @NotEmpty(message = "MA00202.LOT.NOT_EMPTY")
    @Schema(name = "source", example = "private CRC", required = true)
    private String source;

    @NotNull(message = "MA00202.LOT.NOT_NULL")
    @NotEmpty(message = "MA00202.LOT.NOT_EMPTY")
    @Schema(name = "race", example = "FC1 X FC2", required = true)
    private String race;
    //@Min(value =1,  message = "MA00202.LOT.MIN_VALUE")
    @Schema(name = "dflCount", example = "how many dfls", required = true)
    private int dflCount;

    //@Min(value =1,  message = "MA00202.LOT.MIN_VALUE")
    @Schema(name = "estimatedWeight", example = "Estimated weight from farmer", required = true)
    private int estimatedWeight;


    @Schema(name = "status", example = "Generated status field")
    private String status;

    //@Min(value = 1, message = "MA00202.LOT.MIN_VALUE")
    @Schema(name = "numberOfLot", example = "number of lots assigned to this transaction", required = true)
    private int numberOfLot;


    @Schema(name = "numberOfSmallBin", example = "number of small bins assigned to this transaction", required = true)
    private int numberOfSmallBin;

    @Schema(name = "numberOfBigBin", example = "number of big bins assigned to this transaction", required = true)
    private int numberOfBigBin;


    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @PresentDate(message = "MA00212.LOT.PRESENT_DATE")
    private LocalDate marketAuctionDate;

    @Override
    public List<ValidationMessage> validate(){
        List<ValidationMessage> validationMessageList = new ArrayList<ValidationMessage>();

        return validationMessageList;
    }
}
