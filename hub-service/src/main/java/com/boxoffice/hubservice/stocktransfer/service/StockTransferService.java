package com.boxoffice.hubservice.stocktransfer.service;

import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.common.util.PageableUtils;
import com.boxoffice.hubservice.client.BulkHubTransferRequestDto;
import com.boxoffice.hubservice.client.BulkStockCountRequestDto;
import com.boxoffice.hubservice.client.BulkStockCountResponseDto;
import com.boxoffice.hubservice.client.CompanyDetailResponseDto;
import com.boxoffice.hubservice.client.ProductFeignClient;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCompleteRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCreateRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.AssignedCompanyResponseDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.StockTransferResponseDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.SuggestedTransferResponseDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.TransferPlanResponseDto;
import com.boxoffice.hubservice.stocktransfer.entity.QStockTransfer;
import com.boxoffice.hubservice.stocktransfer.entity.StockTransfer;
import com.boxoffice.hubservice.stocktransfer.entity.TransferStatus;
import com.boxoffice.hubservice.stocktransfer.event.TransferDispatchedEvent;
import com.boxoffice.hubservice.stocktransfer.repository.StockTransferRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransferService {

    private final StockTransferRepository stockTransferRepository;
    private final HubRepository hubRepository;
    private final HubRouteRepository hubRouteRepository;
    private final ProductFeignClient productFeignClient;
    private final ApplicationEventPublisher eventPublisher;

    private record Candidate(Hub hub, BigDecimal distanceKm, int availableCapacity) {
    }

    public TransferPlanResponseDto getTransferPlan(UUID fromHubId) {
        Hub fromHub = hubRepository.findById(fromHubId)
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (!fromHub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_NOT_INACTIVE_FOR_TRANSFER);
        }

        List<CompanyDetailResponseDto> companies = fetchCompanies(fromHubId);
        if (companies.isEmpty()) {
            throw new BaseException(HubErrorCode.NO_COMPANIES_TO_TRANSFER);
        }

        int totalStock = companies.stream().mapToInt(CompanyDetailResponseDto::stockCount).sum();
        List<Candidate> candidates = buildCandidates(fromHubId);
        Map<UUID, List<CompanyDetailResponseDto>> assignment = runFFD(companies, candidates);

        List<SuggestedTransferResponseDto> suggestions = candidates.stream()
                .filter(c -> assignment.containsKey(c.hub().getId()))
                .map(c -> {
                    List<CompanyDetailResponseDto> assigned = assignment.get(c.hub().getId());
                    int suggestedCount = assigned.stream().mapToInt(CompanyDetailResponseDto::stockCount).sum();
                    List<AssignedCompanyResponseDto> companyDtos = assigned.stream()
                            .map(co -> new AssignedCompanyResponseDto(
                                    co.companyId(), co.companyName(), co.stockCount()))
                            .toList();
                    return new SuggestedTransferResponseDto(
                            c.hub().getId(), c.hub().getName(),
                            c.distanceKm(), c.availableCapacity(), suggestedCount, companyDtos);
                })
                .toList();

        return new TransferPlanResponseDto(fromHub.getId(), fromHub.getName(), totalStock, suggestions);
    }

    @Transactional
    public List<StockTransferResponseDto> createTransfer(StockTransferCreateRequestDto request) {
        Hub fromHub = hubRepository.findById(request.fromHubId())
                .orElseThrow(() -> new BaseException(HubErrorCode.HUB_NOT_FOUND));

        if (!fromHub.isInactive()) {
            throw new BaseException(HubErrorCode.HUB_NOT_INACTIVE_FOR_TRANSFER);
        }

        if (stockTransferRepository.existsByFromHubIdAndStatusIn(
                request.fromHubId(),
                List.of(TransferStatus.PENDING, TransferStatus.IN_PROGRESS))) {
            throw new BaseException(HubErrorCode.TRANSFER_ALREADY_EXISTS);
        }

        List<CompanyDetailResponseDto> companies = fetchCompanies(request.fromHubId());
        if (companies.isEmpty()) {
            throw new BaseException(HubErrorCode.NO_COMPANIES_TO_TRANSFER);
        }

        List<Candidate> candidates = buildCandidates(request.fromHubId());
        Map<UUID, List<CompanyDetailResponseDto>> assignment = runFFD(companies, candidates);

        List<StockTransfer> transfers = new ArrayList<>();
        for (Map.Entry<UUID, List<CompanyDetailResponseDto>> entry : assignment.entrySet()) {
            List<CompanyDetailResponseDto> assigned = entry.getValue();
            List<UUID> companyIds = assigned.stream().map(CompanyDetailResponseDto::companyId).toList();
            int totalProductCount = assigned.stream().mapToInt(CompanyDetailResponseDto::stockCount).sum();

            StockTransfer transfer = StockTransfer.builder()
                    .fromHubId(request.fromHubId())
                    .toHubId(entry.getKey())
                    .totalProductCount(totalProductCount)
                    .managerId(fromHub.getManagerId())
                    .companyIds(companyIds)
                    .build();

            transfers.add(stockTransferRepository.save(transfer));
        }

        return transfers.stream().map(StockTransferResponseDto::from).toList();
    }

    public StockTransferResponseDto getTransfer(UUID transferId) {
        return StockTransferResponseDto.from(findTransferById(transferId));
    }

    public PageResponse<StockTransferResponseDto> getTransfers(
            TransferStatus status, UUID fromHubId, UUID toHubId, int page, int size) {
        Pageable pageable = PageableUtils.ofDefault(page, size);
        QStockTransfer q = QStockTransfer.stockTransfer;
        BooleanBuilder builder = new BooleanBuilder();
        if (status != null) {
            builder.and(q.status.eq(status));
        }
        if (fromHubId != null) {
            builder.and(q.fromHubId.eq(fromHubId));
        }
        if (toHubId != null) {
            builder.and(q.toHubId.eq(toHubId));
        }
        Page<StockTransferResponseDto> result = stockTransferRepository.findAll(builder, pageable)
                .map(StockTransferResponseDto::from);
        return PageResponse.of(result);
    }

    public PageResponse<StockTransferResponseDto> getTransfersByHub(
            TransferStatus status, UUID hubId, int page, int size) {
        Pageable pageable = PageableUtils.ofDefault(page, size);
        QStockTransfer q = QStockTransfer.stockTransfer;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(q.fromHubId.eq(hubId).or(q.toHubId.eq(hubId)));
        if (status != null) {
            builder.and(q.status.eq(status));
        }
        Page<StockTransferResponseDto> result = stockTransferRepository.findAll(builder, pageable)
                .map(StockTransferResponseDto::from);
        return PageResponse.of(result);
    }

    public PageResponse<StockTransferResponseDto> getTransfersByDeliveryManager(
            TransferStatus status, UUID deliveryManagerId, int page, int size) {
        Pageable pageable = PageableUtils.ofDefault(page, size);
        QStockTransfer q = QStockTransfer.stockTransfer;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(q.deliveryManagerId.eq(deliveryManagerId));
        if (status != null) {
            builder.and(q.status.eq(status));
        }
        Page<StockTransferResponseDto> result = stockTransferRepository.findAll(builder, pageable)
                .map(StockTransferResponseDto::from);
        return PageResponse.of(result);
    }

    @Transactional
    public StockTransferResponseDto dispatch(UUID transferId, UUID hubId) {
        StockTransfer transfer = findTransferById(transferId);
        if (!transfer.getFromHubId().equals(hubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BaseException(HubErrorCode.TRANSFER_INVALID_STATUS);
        }
        if (stockTransferRepository.countByFromHubIdAndStatus(
                transfer.getFromHubId(), TransferStatus.IN_PROGRESS) > 0) {
            throw new BaseException(HubErrorCode.TRANSFER_ALREADY_IN_PROGRESS);
        }
        transfer.dispatch();
        eventPublisher.publishEvent(
                new TransferDispatchedEvent(transferId, transfer.getFromHubId(), transfer.getToHubId()));
        return StockTransferResponseDto.from(transfer);
    }

    @Transactional
    public StockTransferResponseDto completeByDeliveryManager(
            UUID transferId, UUID deliveryManagerId, StockTransferCompleteRequestDto request) {
        StockTransfer transfer = findTransferById(transferId);
        if (transfer.getDeliveryManagerId() == null) {
            throw new BaseException(HubErrorCode.TRANSFER_INVALID_STATUS);
        }
        if (!deliveryManagerId.equals(transfer.getDeliveryManagerId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        completeTransfer(transfer, request);
        return StockTransferResponseDto.from(transfer);
    }


    @Transactional
    public StockTransferResponseDto complete(
            UUID transferId, UUID hubId, StockTransferCompleteRequestDto request) {
        StockTransfer transfer = findTransferById(transferId);
        if (!transfer.getToHubId().equals(hubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        completeTransfer(transfer, request);
        return StockTransferResponseDto.from(transfer);
    }

    private void completeTransfer(StockTransfer transfer, StockTransferCompleteRequestDto request) {
        if (transfer.getStatus() == TransferStatus.COMPLETED) {
            throw new BaseException(HubErrorCode.TRANSFER_ALREADY_COMPLETED);
        }
        if (transfer.getStatus() != TransferStatus.IN_PROGRESS) {
            throw new BaseException(HubErrorCode.TRANSFER_INVALID_STATUS);
        }
        transfer.complete(request != null ? request.note() : null);
        productFeignClient.bulkHubTransfer(
                new BulkHubTransferRequestDto(transfer.getCompanyIds(), transfer.getToHubId()));
    }

    @Transactional
    public StockTransferResponseDto cancel(UUID transferId) {
        StockTransfer transfer = findTransferById(transferId);
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BaseException(HubErrorCode.TRANSFER_INVALID_STATUS);
        }
        transfer.cancel();
        return StockTransferResponseDto.from(transfer);
    }

    @Transactional
    public StockTransferResponseDto cancel(UUID transferId, UUID hubId) {
        StockTransfer transfer = findTransferById(transferId);
        if (!transfer.getFromHubId().equals(hubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw new BaseException(HubErrorCode.TRANSFER_INVALID_STATUS);
        }
        transfer.cancel();
        return StockTransferResponseDto.from(transfer);
    }

    private List<Candidate> buildCandidates(UUID fromHubId) {
        List<HubRoute> routes = hubRouteRepository.findAllByOriginHubId(fromHubId);
        List<UUID> destinationIds = routes.stream().map(HubRoute::getDestinationHubId).toList();

        Map<UUID, Hub> hubMap = hubRepository.findAllById(destinationIds)
                .stream().collect(Collectors.toMap(Hub::getId, h -> h));

        Map<UUID, Integer> stockCounts = fetchBulkStockCount(destinationIds);

        return routes.stream()
                .map(route -> {
                    Hub toHub = hubMap.get(route.getDestinationHubId());
                    if (toHub == null || toHub.isInactive() || toHub.isClosing() || toHub.getCapacity() == null) {
                        return null;
                    }
                    int available = toHub.getCapacity() - stockCounts.getOrDefault(toHub.getId(), 0);
                    if (available <= 0) {
                        return null;
                    }
                    return new Candidate(toHub, route.getEstimatedDistanceKm(), available);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingDouble(c ->
                        -(c.availableCapacity() / c.distanceKm().doubleValue())))
                .toList();
    }

    private Map<UUID, List<CompanyDetailResponseDto>> runFFD(
            List<CompanyDetailResponseDto> companies, List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            throw new BaseException(HubErrorCode.TRANSFER_EXCEEDS_CAPACITY);
        }

        List<CompanyDetailResponseDto> sorted = companies.stream()
                .sorted(Comparator.comparingInt(CompanyDetailResponseDto::stockCount).reversed())
                .toList();

        Map<UUID, Integer> remaining = candidates.stream()
                .collect(Collectors.toMap(c -> c.hub().getId(), Candidate::availableCapacity));

        Map<UUID, List<CompanyDetailResponseDto>> assignment = new LinkedHashMap<>();
        candidates.forEach(c -> assignment.put(c.hub().getId(), new ArrayList<>()));

        for (CompanyDetailResponseDto company : sorted) {
            boolean assigned = false;
            for (Candidate candidate : candidates) {
                UUID hubId = candidate.hub().getId();
                int cap = remaining.getOrDefault(hubId, 0);
                if (company.stockCount() <= cap) {
                    assignment.get(hubId).add(company);
                    remaining.put(hubId, cap - company.stockCount());
                    assigned = true;
                    break;
                }
            }
            if (!assigned) {
                throw new BaseException(HubErrorCode.TRANSFER_EXCEEDS_CAPACITY);
            }
        }

        assignment.entrySet().removeIf(e -> e.getValue().isEmpty());
        return assignment;
    }

    private List<CompanyDetailResponseDto> fetchCompanies(UUID hubId) {
        ApiResponse<List<CompanyDetailResponseDto>> response = productFeignClient.getCompaniesByHubId(hubId);
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData();
    }

    private Map<UUID, Integer> fetchBulkStockCount(List<UUID> hubIds) {
        if (hubIds.isEmpty()) {
            return Map.of();
        }
        ApiResponse<List<BulkStockCountResponseDto>> response =
                productFeignClient.getBulkStockCount(new BulkStockCountRequestDto(hubIds));
        if (response == null || response.getData() == null) {
            return Map.of();
        }
        return response.getData().stream()
                .collect(Collectors.toMap(BulkStockCountResponseDto::hubId, BulkStockCountResponseDto::stockCount));
    }

    private StockTransfer findTransferById(UUID transferId) {
        return stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new BaseException(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    public StockTransferResponseDto getTransferByHub(UUID transferId, UUID hubId) {
        StockTransfer transfer = findTransferById(transferId);
        if (!transfer.getFromHubId().equals(hubId) && !transfer.getToHubId().equals(hubId)) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        return StockTransferResponseDto.from(transfer);
    }

    public StockTransferResponseDto getTransferByDeliveryManager(UUID transferId, UUID deliveryManagerId) {
        StockTransfer transfer = findTransferById(transferId);
        if (!deliveryManagerId.equals(transfer.getDeliveryManagerId())) {
            throw new BaseException(CommonErrorCode.FORBIDDEN);
        }
        return StockTransferResponseDto.from(transfer);
    }
}
