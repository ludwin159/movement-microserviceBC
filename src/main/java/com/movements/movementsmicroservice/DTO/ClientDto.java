package com.movements.movementsmicroservice.DTO;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

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
    public enum TypeClient {
        BUSINESS_CLIENT, PERSONAL_CLIENT, BUSINESS_PYMES_CLIENT, PERSONAL_VIP_CLIENT
    }
}
