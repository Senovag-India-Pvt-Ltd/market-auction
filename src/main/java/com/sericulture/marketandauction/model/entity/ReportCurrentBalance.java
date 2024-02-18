package com.sericulture.marketandauction.model.entity;


import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class ReportCurrentBalance {

    Double currentBalance;

    String virtualAccountNumber;

    LocalDateTime createdDate;



}
