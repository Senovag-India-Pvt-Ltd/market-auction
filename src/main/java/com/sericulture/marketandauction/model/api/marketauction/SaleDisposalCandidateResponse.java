package com.sericulture.marketandauction.model.api.marketauction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sericulture.marketandauction.model.api.ResponseBody;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

/**
 * One selectable disposal row returned to the UI when a single fruitsId + lotNumber
 * maps to more than one sale_and_disposal_of_dfls record. The user picks one of these
 * and the chosen {@code saleDisposalId} is sent back so the backend marks only that row
 * as disposed (is_disposed = 1).
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleDisposalCandidateResponse extends ResponseBody {

    @Schema(name = "saleDisposalId", example = "5")
    private Integer saleDisposalId;

    @Schema(name = "fruitsId", example = "F12345")
    private String fruitsId;

    @Schema(name = "lotNumber", example = "10")
    private String lotNumber;

    @Schema(name = "numberOfDflsDisposed", example = "100")
    private Integer numberOfDflsDisposed;

    @Schema(name = "eggSheetNumbers", example = "ES-001,ES-002")
    private String eggSheetNumbers;

    @Schema(name = "raceId", example = "2")
    private Integer raceId;

    @Schema(name = "releaseDate", example = "2024-07-01")
    private LocalDate releaseDate;

    @Schema(name = "dateOfDisposal", example = "2024-07-08")
    private LocalDate dateOfDisposal;

    @Schema(name = "nameAndAddressOfTheFarm", example = "ABC Farm, Bengaluru")
    private String nameAndAddressOfTheFarm;

    @Schema(name = "invoiceNumber", example = "INV/SALE/01")
    private String invoiceNumber;

    @Schema(name = "receiptNo", example = "RC-001")
    private String receiptNo;

    @Schema(name = "isDisposed", example = "0",
            description = "0 = not yet disposed, 1 = already disposed")
    private Integer isDisposed;
}
