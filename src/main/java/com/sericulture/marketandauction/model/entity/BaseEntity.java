package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;

import java.util.Date;
@MappedSuperclass
@FilterDef(name = "activeEducationFilter", parameters = @ParamDef(name = "active", type = Boolean.class))
public class BaseEntity {

    @Column(name = "CREATED_BY")
    @GenericGenerator(name = "uuid", strategy = "uuid2") //TODO once security is added we need to add the user if from principal
    private int createdBy;

    @GenericGenerator(name = "uuid", strategy = "uuid2") //TODO once security is added we need to add the user if from principal
    @Column(name = "MODIFIED_BY")
    private int modifiedBy;
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CREATED_DATE")
    private Date createdDate;
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    @Getter
    @Setter
    @Column(name = "ACTIVE", columnDefinition = "TINYINT")
    private Boolean active;

    @PrePersist
    public void prePersist() {
        if(active == null)
            active = true;
    }

    @PreUpdate
    public void preUpdate() {
        if(active == null)
            active = true;
    }
}
