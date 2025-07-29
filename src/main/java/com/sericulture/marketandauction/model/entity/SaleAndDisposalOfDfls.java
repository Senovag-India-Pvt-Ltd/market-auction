package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "sale_and_disposal_of_dfls")
public class SaleAndDisposalOfDfls extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sale_and_disposal_of_dfls_seq")
    @SequenceGenerator(name = "sale_and_disposal_of_dfls_seq", sequenceName = "sale_and_disposal_of_dfls_seq", allocationSize = 1)
    Integer id;

    private Long userMasterId;

    private String lotNumber;

    private String eggSheetNumbers;

    private Integer raceId;

    private LocalDate releaseDate;

    private LocalDate dateOfDisposal;

    private LocalDate expectedDateOfHatching;

    private Integer numberOfDflsDisposed;

    private String fruitsId;

    // farm, farmer, crc
    private String userType;

    private Long userTypeId;

    private String nameAndAddressOfTheFarm;

    private Double ratePer100DflsPrice;

    private String invoiceNumber;

    private Long lineNameId;

    private Long generationNumberId;

    //is_accepted : 0 = not accepted, 1 = accepted, 2 = rejected | means all the buyer bought it
    @Column(name = "is_accepted")
    private int isAccepted;

    //No need to return in get-info api, only for receipt of dfls
    private LocalDate laidOnDate;
    private Long grainageId;

    @Column(name = "is_sale_tracked")
    private Integer isSaleTracked;

    @Column(name = "is_verified")
    private Integer isVerified;

    @Column(name = "receipt_no")
    private String receiptNo;

    @Column(name = "price")
    private Float price;

    @Column(name = "sold_after_1st_or_2nd_mould")
    private String soldAfter1stOr2ndMould;

    @Column(name = "tsc")
    private Long tsc;

    @Column(name = "source_of_dfls")
    private String dflsSource;

    @Column(name = "is_disposed")
    private Integer isDisposed;
}
