package com.jeezus.gatex.management.dto.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TenantOrganizationRequest {
    // cant change slug , only username can change
    @NotBlank
    @Size(max = 150)
    private String name;
}
