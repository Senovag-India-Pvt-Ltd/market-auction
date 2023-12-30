package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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
public class UserMaster extends BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_master_seq")
    @SequenceGenerator(name = "user_master_seq", sequenceName = "user_master_seq", allocationSize = 1)
    @Column(name = "user_master_id")
    private Long userMasterId;


    @Size(min = 2, max = 250, message = "First name should be more than 1 characters.")
    @Column(name = "first_name", unique = true)
    private String firstName;

    //    @Size(min = 2, max = 250, message = "Middle name should be more than 1 characters.")
    @Column(name = "middle_name", unique = false)
    private String middleName;

    //    @Size(min = 2, max = 250, message = "Last name should be more than 1 characters.")
    @Column(name = "last_name", unique = false)
    private String lastName;

    @Size(min = 2, max = 250, message = "password name should be more than 1 characters.")
    @Column(name = "password", unique = false)
    private String password;

    //    @Size(min = 2, max = 250, message = "Email name should be more than 1 characters.")
    @Column(name = "email_id", unique = false)
    private String emailID;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "taluk_id")
    private Long talukId;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "market_id")
    private Long marketMasterId;

    @Size(min = 2, max = 250, message = "Username name should be more than 1 characters.")
    @Column(name = "username", unique = true)
    private String username;

    @Column(name = "designation_id")
    private Long designationId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "user_type")
    private int userType;

    @Column(name = "user_type_id")
    private Long userTypeId;

    @Column(name = "device_id")
    private String deviceId;
}
