package com.sericulture.marketandauction.model.api.marketauction;

import com.sericulture.marketandauction.model.api.RequestBody;

import lombok.*;

import java.time.LocalDate;


@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ExceptionalTimeRequest extends RequestBody {

    private String issueBidSlipStartTime;

    private String issueBidSlipEndTime;

    private String auction1StartTime;

    private String auction2StartTime;

    private String auction3StartTime;

    private String auction1EndTime;

    private String auction2EndTime;

    private String auction3EndTime;

    private String auction1AcceptStartTime;

    private String auction2AcceptStartTime;

    private String auction3AcceptStartTime;

    private String auction1AcceptEndTime;

    private String auction2AcceptEndTime;

    private String auction3AcceptEndTime;
}
