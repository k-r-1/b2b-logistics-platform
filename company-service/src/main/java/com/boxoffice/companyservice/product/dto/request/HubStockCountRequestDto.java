package com.boxoffice.companyservice.product.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class HubStockCountRequestDto {

    @NotEmpty(message = "허브 ID 목록은 필수입니다.")
    private List<@NotNull(message = "허브 ID는 null일 수 없습니다.") UUID> hubIds;
}
