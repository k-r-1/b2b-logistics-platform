package com.boxoffice.hubservice.hub.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    @Transactional
    public HubCreateResponseDto createHub(HubCreateRequestDto request) {
        if (request.hubType() == HubType.INACTIVE) {
            throw new BaseException(HubErrorCode.INVALID_HUB_TYPE);
        }

        if (hubRepository.existsByName(request.name())) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        Hub hub = Hub.builder()
                .name(request.name())
                .address(new AddressVO(request.zipCode(), request.address(), request.detailAddress()))
                .coordinate(new CoordinateVO(request.latitude(), request.longitude()))
                .hubType(request.hubType())
                .capacity(request.capacity())
                .build();

        return HubCreateResponseDto.from(hubRepository.save(hub));
    }
}