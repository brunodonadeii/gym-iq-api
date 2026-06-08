package com.gymiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AuditFilterOptionsResponse {

    private List<AuditFilterOptionResponse> actions;
    private List<AuditFilterOptionResponse> resourceTypes;
    private List<AuditActorOptionResponse> actors;
}
