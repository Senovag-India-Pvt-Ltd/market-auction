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
public class ExternalUnitRegistration extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "EXTERNAL_UNIT_REGISTRATION_SEQ")
    @SequenceGenerator(name = "EXTERNAL_UNIT_REGISTRATION_SEQ", sequenceName = "EXTERNAL_UNIT_REGISTRATION_SEQ", allocationSize = 1)
    @Column(name = "EXTERNAL_UNIT_REGISTRATION_ID")
    private Long externalUnitRegistrationId;

    @Column(name = "external_unit_type_id")
    private Long externalUnitTypeId;

    @Column(name = "user_master_id")
    private Long userMasterId;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "license_number")
    private String licenseNumber;

    @Column(name = "external_unit_number")
    private String externalUnitNumber;

    @Column(name = "organisation_name")
    private String organisationName;

    @Column(name = "race_id")
    private Long raceMasterId;

    @Column(name = "capacity")
    private String capacity;

    @Column(name = "market_master_id")
    private Long marketMasterId;

    @Column(name = "virtual_account_number")
    private String virtualAccountNumber;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "lot_number_nomenclature")
    private String lotNumberNomenclature;
}