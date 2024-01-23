package com.sericulture.marketandauction.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.springframework.security.core.context.SecurityContextHolder;

import java.security.Principal;
import java.util.Date;
@MappedSuperclass
@FilterDef(name = "activeEducationFilter", parameters = @ParamDef(name = "active", type = Boolean.class))
public class BaseEntity {

    @Column(name = "CREATED_BY")
    private String createdBy = "";
    @Column(name = "MODIFIED_BY")
    private String modifiedBy ="";
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
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        this.createdBy = principal.getName();
        this.modifiedBy = principal.getName();
    }

    @PreUpdate
    public void preUpdate() {
        if(active == null)
            active = true;
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        this.modifiedBy = principal.getName();
    }
}
