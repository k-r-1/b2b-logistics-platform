package com.boxoffice.companyservice.company.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BulkHubTransferRequestDto {

    @NotEmpty(message = "업체 ID 목록은 필수입니다.")
    private List<@NotNull(message = "업체 ID는 null일 수 없습니다.") UUID> companyIds;

    @NotNull(message = "이전 대상 허브 ID는 필수입니다.")
    private UUID toHubId;
}
