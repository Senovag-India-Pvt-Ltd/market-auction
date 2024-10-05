package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PupaTestAndCocoonAssessment extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PUPA_TEST_AND_COCOON_ASSESSMENT_SEQ")
    @SequenceGenerator(name = "PUPA_TEST_AND_COCOON_ASSESSMENT_SEQ", sequenceName = "PUPA_TEST_AND_COCOON_ASSESSMENT_SEQ", allocationSize = 1)
    @Column(name = "PUPA_TEST_AND_COCOON_ASSESSMENT_ID")
    private int id;
    private int marketAuctionId;
    private LocalDate testDate;
    private int noOfCocoonTakenForExamination;
    private int noOfDflFromFc;
    private boolean diseaseFree;
    private String diseaseType;
    private int noOfCocoonPerKg;
    private float meltPercentage;
    private String pupaTestResult;
    private String cocoonAssessmentResult;
}
