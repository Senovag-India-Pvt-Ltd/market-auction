package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FitnessCertificate extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fitness_certificate_seq")
    @SequenceGenerator(name = "fitness_certificate_seq", sequenceName = "fitness_certificate_seq", allocationSize = 1)

    @Column(name = "fitness_certificate_id")
    private Long fitnessCertificateId;

    @Column(name = "chowki_id ")
    private Long chowkiId;

    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "expected_cocoon")
    private Float expectedCocoon;

    @Column(name = "lot_test_details")
    private String lotTestDetails;

    @Column(name = "disease_status_id")
    private Long diseaseStatusId;

    @Column(name = "no_of_chandies")
    private Float noOfChandies;

    @Column(name = "expected_marker_date")
    private String spunFromDate;

    @Column(name = "spun_date")
    private String spunToDate;

    @Column(name = "fitness_certificate_path")
    private String fitnessCertificatePath;

    @Column(name = "fruits_id")
    private String fruitsId;

    @Column(name = "sale_and_disposal_id")
    private Long  saleAndDisposalId;

    @Column(name = "is_fc_issued")
    private Integer isFcIssued;
}
