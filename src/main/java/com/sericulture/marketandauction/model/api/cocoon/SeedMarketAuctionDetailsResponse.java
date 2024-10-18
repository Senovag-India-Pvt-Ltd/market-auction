package com.sericulture.marketandauction.model.api.cocoon;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class SeedMarketAuctionDetailsResponse {
    private int serialNumber;
    private Long farmerId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String farmerNumber;
    private String fruitsId;
    private String tscName;
    private String passbookNumber;
    private String fatherName;
    private String address;
    private String dob;
    private String gender;
    private String casteTitle;
    private String mobileNumber;
    private String arnNumber;
    private String stateName;
    private String districtName;
    private String talukName;
    private String hobliName;
    private String villageName;
    private String dflsSource;
    private String numbersOfDfls;
    private String lotNumberRsp;
    private Long raceOfDfls;
    private String raceName;
    private Long initialWeighment;
    private Long marketAuctionId;
    private Long pricePerKg;
    private String fixationDate;
    private String testDate;
    private Long noOfCocoonTakenForExamination;
    private Long noOfDFlFromFc;
    private String diseaseFree;
    private String diseaseType;
    private Long noOfCocoonPerKg;
    private String pupaCocoonStatus;
    private String meltPercentage;
    private String noOfCocoonsExamined;
    private String marketAuctionDate;
    private String fitnessCertificatePath;
}
