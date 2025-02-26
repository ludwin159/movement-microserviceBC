package com.movements.movementsmicroservice.DTO;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class ClientDto {
    private String id;
    private String identity;
    private String fullName;
    private String taxId;
    private String businessName;
    private String address;
    private String phone;
    private String email;
    private TypeClient typeClient;
    private Date disbursementDate;
    private Date firstDatePay;
    private Integer totalMonths;
    private Double monthlyFee;
    private Double penaltyFee;
    public enum TypeClient {
        BUSINESS_CLIENT, PERSONAL_CLIENT, BUSINESS_PYMES_CLIENT, PERSONAL_VIP_CLIENT
    }
}
