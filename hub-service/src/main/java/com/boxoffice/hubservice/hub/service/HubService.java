package com.boxoffice.hubservice.hub.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubGetResponseDto;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.entity.QHub;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HubService {

    private final HubRepository hubRepository;

    @Transactional
    public HubCreateResponseDto createHub(HubCreateRequestDto request) {
        if (request.hubType() == HubType.INACTIVE || request.hubType() == HubType.CLOSING) {
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

    @Cacheable(cacheNames = "hub", key = "#hubId")
    public HubGetResponseDto getHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        return HubGetResponseDto.from(hub);
    }

    public PageResponse<HubGetResponseDto> getHubs(String name, HubType hubType, int page, int size) {
        Pageable pageable = PageableUtils.ofDefault(page, size);

        QHub qHub = QHub.hub;
        BooleanBuilder builder = new BooleanBuilder();
        if (name != null && !name.isBlank()) {
            builder.and(qHub.name.containsIgnoreCase(name));
        }
        if (hubType != null) {
            builder.and(qHub.hubType.eq(hubType));
        }

        Page<HubGetResponseDto> result = hubRepository.findAll(builder, pageable).map(HubGetResponseDto::from);
        return PageResponse.of(result);
    }
}