package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressLookupResponse {

    private String zipCode;
    private String street;
    private String neighborhood;
    private String city;
    private String state;
    private String formattedAddress;
}
