package com.sericulture.marketandauction.model.api.marketauction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Lookup payload the UI sends to fetch all disposal rows matching a fruitsId + lotNumber,
 * so it can show them and let the user pick which one to mark as disposed. The field names
 * mirror {@link LotGroupageRequest} (lotParentLevel is used as the disposal lotNumber and
 * dflLotNumber as numberOfDflsDisposed) so the candidate set matches the save-time lookup.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SaleDisposalLookupRequest {

    @Schema(name = "fruitsId", example = "F12345")
    private String fruitsId;

    @Schema(name = "lotParentLevel", example = "10")
    private String lotParentLevel;

    @Schema(name = "dflLotNumber", example = "100")
    private Long dflLotNumber;
}
