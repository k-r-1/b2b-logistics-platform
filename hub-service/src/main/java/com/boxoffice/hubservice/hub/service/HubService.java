package com.boxoffice.hubservice.hub.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.dto.request.HubCreateRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubClosingRequestDto;
import com.boxoffice.hubservice.hub.dto.request.HubUpdateRequestDto;
import com.boxoffice.hubservice.hub.dto.response.HubActiveResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubCreateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubDeactivateResponseDto;
import com.boxoffice.hubservice.hub.dto.response.HubGetResponseDto;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.entity.QHub;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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

    @Transactional
    @CacheEvict(cacheNames = "hub", key = "#hubId")
    public HubGetResponseDto updateHub(UUID hubId, HubUpdateRequestDto request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (hub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_INACTIVE);
        }
        if (hub.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_CLOSING);
        }

        if (request.name() != null && hubRepository.existsByNameAndIdNot(request.name(), hubId)) {
            throw new BaseException(HubErrorCode.DUPLICATE_HUB_NAME);
        }

        boolean hasAddressChange = request.zipCode() != null
                || request.address() != null
                || request.detailAddress() != null;

        AddressVO address = null;
        if (hasAddressChange) {
            address = new AddressVO(
                    request.zipCode() != null ? request.zipCode() : hub.getAddress().getZipCode(),
                    request.address() != null ? request.address() : hub.getAddress().getAddress(),
                    request.detailAddress() != null ? request.detailAddress() : hub.getAddress().getDetailAddress()
            );
        }

        CoordinateVO coordinate = null;
        if (request.latitude() != null && request.longitude() != null) {
            coordinate = new CoordinateVO(request.latitude(), request.longitude());
        }

        hub.update(request.name(), address, coordinate, request.capacity());
        return HubGetResponseDto.from(hub);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "hub", key = "#hubId"),
            @CacheEvict(cacheNames = "hub-routes", allEntries = true)
    })
    public HubGetResponseDto startClosingHub(UUID hubId, HubClosingRequestDto request) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (hub.getHubType() == HubType.CENTRAL) {
            throw new BaseException(HubErrorCode.CENTRAL_HUB_CANNOT_CLOSE);
        }

        if (hub.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_ALREADY_CLOSING);
        }

        if (hub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_ALREADY_INACTIVE);
        }

        hub.startClosing(request.reason());
        return HubGetResponseDto.from(hub);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "hub", key = "#hubId"),
            @CacheEvict(cacheNames = "hub-routes", allEntries = true)
    })
    public HubDeactivateResponseDto deactivateHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (hub.getHubType() == HubType.CENTRAL) {
            throw new BaseException(HubErrorCode.CENTRAL_HUB_CANNOT_DEACTIVATE);
        }

        if (hub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_ALREADY_INACTIVE);
        }

        if (!hub.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_NOT_CLOSING);
        }

        hub.deactivate();
        return HubDeactivateResponseDto.from(hub);
    }

    public HubActiveResponseDto getActiveHub(UUID hubId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        if (hub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_INACTIVE);
        }
        if (hub.isClosing()) {
            throw new BaseException(HubErrorCode.HUB_CLOSING);
        }
        return new HubActiveResponseDto(hub.getId(), hub.isActive());
    }

    @Transactional
    @CacheEvict(cacheNames = "hub", key = "#hubId")
    public HubGetResponseDto assignManager(UUID hubId, UUID managerId) {
        Hub hub = hubRepository.findById(hubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));
        hub.assignManager(managerId);
        return HubGetResponseDto.from(hub);
    }
}
