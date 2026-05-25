package com.boxoffice.hubservice.stocktransfer.service;

import com.boxoffice.common.entity.AddressVO;
import com.boxoffice.common.exception.BaseException;
import com.boxoffice.common.exception.CommonErrorCode;
import com.boxoffice.common.response.ApiResponse;
import com.boxoffice.common.response.PageResponse;
import com.boxoffice.hubservice.client.BulkStockCountResponseDto;
import com.boxoffice.hubservice.client.CompanyDetailResponseDto;
import com.boxoffice.hubservice.client.ProductFeignClient;
import com.boxoffice.hubservice.exception.HubErrorCode;
import com.boxoffice.hubservice.hub.entity.CoordinateVO;
import com.boxoffice.hubservice.hub.entity.Hub;
import com.boxoffice.hubservice.hub.entity.HubType;
import com.boxoffice.hubservice.hub.repository.HubRepository;
import com.boxoffice.hubservice.hubroute.entity.HubRoute;
import com.boxoffice.hubservice.hubroute.repository.HubRouteRepository;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCompleteRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.request.StockTransferCreateRequestDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.StockTransferResponseDto;
import com.boxoffice.hubservice.stocktransfer.dto.response.TransferPlanResponseDto;
import com.boxoffice.hubservice.stocktransfer.entity.StockTransfer;
import com.boxoffice.hubservice.stocktransfer.entity.TransferStatus;
import com.boxoffice.hubservice.stocktransfer.kafka.StockTransferKafkaProducer;
import com.boxoffice.hubservice.stocktransfer.repository.StockTransferRepository;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransferServiceTest {

    @InjectMocks
    private StockTransferService stockTransferService;

    @Mock
    private StockTransferRepository stockTransferRepository;

    @Mock
    private HubRepository hubRepository;

    @Mock
    private HubRouteRepository hubRouteRepository;

    @Mock
    private ProductFeignClient productFeignClient;

    @Mock
    private StockTransferKafkaProducer kafkaProducer;

    private Hub buildHub(UUID id, HubType hubType, Integer capacity) {
        Hub hub = Hub.builder()
                .name("테스트 허브 " + id)
                .address(new AddressVO("12345", "서울시 강남구", null))
                .coordinate(new CoordinateVO(37.5, 127.0))
                .hubType(hubType)
                .capacity(capacity)
                .build();
        ReflectionTestUtils.setField(hub, "id", id);
        ReflectionTestUtils.setField(hub, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(hub, "updatedAt", LocalDateTime.now());
        return hub;
    }

    private Hub buildHub(HubType hubType, Integer capacity) {
        return buildHub(UUID.randomUUID(), hubType, capacity);
    }

    private HubRoute buildRoute(UUID originHubId, UUID destinationHubId, BigDecimal distanceKm) {
        HubRoute route = HubRoute.builder()
                .originHubId(originHubId)
                .destinationHubId(destinationHubId)
                .estimatedDurationMin(120)
                .estimatedDistanceKm(distanceKm)
                .build();
        ReflectionTestUtils.setField(route, "id", UUID.randomUUID());
        return route;
    }

    private StockTransfer buildTransfer(UUID fromHubId, UUID toHubId, TransferStatus status) {
        StockTransfer transfer = StockTransfer.builder()
                .fromHubId(fromHubId)
                .toHubId(toHubId)
                .totalProductCount(30)
                .companyIds(List.of(UUID.randomUUID()))
                .build();
        ReflectionTestUtils.setField(transfer, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(transfer, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(transfer, "updatedAt", LocalDateTime.now());
        if (status == TransferStatus.IN_PROGRESS) {
            transfer.dispatch();
        } else if (status == TransferStatus.COMPLETED) {
            transfer.dispatch();
            transfer.complete(null);
        } else if (status == TransferStatus.CANCELLED) {
            transfer.cancel();
        }
        return transfer;
    }

    /**
     * buildCandidates() 내부에서 호출되는 3개 의존성을 일괄 목킹한다.
     * available = toHub.capacity - existingStock
     */
    private void givenCandidates(UUID fromHubId, Hub toHub, int existingStock) {
        HubRoute route = buildRoute(fromHubId, toHub.getId(), BigDecimal.valueOf(400));
        given(hubRouteRepository.findAllByOriginHubId(fromHubId)).willReturn(List.of(route));
        given(hubRepository.findAllById(any(Collection.class))).willReturn(List.of(toHub));
        given(productFeignClient.getBulkStockCount(any()))
                .willReturn(ApiResponse.success(List.of(new BulkStockCountResponseDto(toHub.getId(), existingStock))));
    }

    @Test
    @DisplayName("이전 계획 조회 성공")
    void getTransferPlan_success() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);
        Hub toHub = buildHub(HubType.REGIONAL, 100);
        CompanyDetailResponseDto company = new CompanyDetailResponseDto(UUID.randomUUID(), "테스트 업체", 30);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of(company)));
        givenCandidates(fromHubId, toHub, 0);

        TransferPlanResponseDto result = stockTransferService.getTransferPlan(fromHubId);

        assertThat(result.fromHubId()).isEqualTo(fromHubId);
        assertThat(result.totalStock()).isEqualTo(30);
        assertThat(result.suggestedTransfers()).hasSize(1);
        assertThat(result.suggestedTransfers().get(0).toHubId()).isEqualTo(toHub.getId());
        assertThat(result.suggestedTransfers().get(0).suggestedCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("이전 계획 조회 - 출발 허브 없음")
    void getTransferPlan_hubNotFound() {
        UUID fromHubId = UUID.randomUUID();
        given(hubRepository.findById(fromHubId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.getTransferPlan(fromHubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("이전 계획 조회 - 출발 허브가 INACTIVE 아님")
    void getTransferPlan_hubNotInactive() {
        UUID fromHubId = UUID.randomUUID();
        Hub activeHub = buildHub(fromHubId, HubType.REGIONAL, 100);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(activeHub));

        assertThatThrownBy(() -> stockTransferService.getTransferPlan(fromHubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_INACTIVE_FOR_TRANSFER));
    }

    @Test
    @DisplayName("이전 계획 조회 - 이전할 업체 없음")
    void getTransferPlan_noCompanies() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of()));

        assertThatThrownBy(() -> stockTransferService.getTransferPlan(fromHubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.NO_COMPANIES_TO_TRANSFER));
    }

    @Test
    @DisplayName("이전 계획 조회 - 후보 허브 없음으로 용량 초과")
    void getTransferPlan_noCandidates() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);
        CompanyDetailResponseDto company = new CompanyDetailResponseDto(UUID.randomUUID(), "테스트 업체", 30);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of(company)));
        given(hubRouteRepository.findAllByOriginHubId(fromHubId)).willReturn(List.of());

        assertThatThrownBy(() -> stockTransferService.getTransferPlan(fromHubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_EXCEEDS_CAPACITY));
    }

    @Test
    @DisplayName("이전 계획 조회 - 업체 재고가 후보 허브 가용 용량 초과")
    void getTransferPlan_exceedsCapacity() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);
        Hub toHub = buildHub(HubType.REGIONAL, 10);
        CompanyDetailResponseDto company = new CompanyDetailResponseDto(UUID.randomUUID(), "테스트 업체", 100);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of(company)));
        givenCandidates(fromHubId, toHub, 5); // available = 10 - 5 = 5 < 100

        assertThatThrownBy(() -> stockTransferService.getTransferPlan(fromHubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_EXCEEDS_CAPACITY));
    }

    @Test
    @DisplayName("재고 이전 생성 성공")
    void createTransfer_success() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);
        Hub toHub = buildHub(HubType.REGIONAL, 100);
        CompanyDetailResponseDto company = new CompanyDetailResponseDto(UUID.randomUUID(), "테스트 업체", 30);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(stockTransferRepository.existsByFromHubIdAndStatusIn(any(), any())).willReturn(false);
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of(company)));
        givenCandidates(fromHubId, toHub, 0);
        given(stockTransferRepository.save(any(StockTransfer.class)))
                .willAnswer(invocation -> {
                    StockTransfer t = invocation.getArgument(0);
                    ReflectionTestUtils.setField(t, "id", UUID.randomUUID());
                    return t;
                });

        List<StockTransferResponseDto> result =
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fromHubId()).isEqualTo(fromHubId);
        assertThat(result.get(0).toHubId()).isEqualTo(toHub.getId());
        assertThat(result.get(0).status()).isEqualTo(TransferStatus.PENDING);
        verify(stockTransferRepository, times(1)).save(any(StockTransfer.class));
    }

    @Test
    @DisplayName("재고 이전 생성 - 출발 허브 없음")
    void createTransfer_hubNotFound() {
        UUID fromHubId = UUID.randomUUID();
        given(hubRepository.findById(fromHubId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId)))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_FOUND));
    }

    @Test
    @DisplayName("재고 이전 생성 - 출발 허브가 INACTIVE 아님")
    void createTransfer_hubNotInactive() {
        UUID fromHubId = UUID.randomUUID();
        Hub activeHub = buildHub(fromHubId, HubType.REGIONAL, 100);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(activeHub));

        assertThatThrownBy(() ->
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId)))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.HUB_NOT_INACTIVE_FOR_TRANSFER));
    }

    @Test
    @DisplayName("재고 이전 생성 - 이미 진행 중인 이전 존재 (PENDING/IN_PROGRESS)")
    void createTransfer_alreadyExists() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(stockTransferRepository.existsByFromHubIdAndStatusIn(any(), any())).willReturn(true);

        assertThatThrownBy(() ->
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId)))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("재고 이전 생성 - 이전할 업체 없음")
    void createTransfer_noCompanies() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(stockTransferRepository.existsByFromHubIdAndStatusIn(any(), any())).willReturn(false);
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of()));

        assertThatThrownBy(() ->
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId)))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.NO_COMPANIES_TO_TRANSFER));
    }

    @Test
    @DisplayName("재고 이전 생성 - 용량 초과")
    void createTransfer_exceedsCapacity() {
        UUID fromHubId = UUID.randomUUID();
        Hub fromHub = buildHub(fromHubId, HubType.INACTIVE, null);
        Hub toHub = buildHub(HubType.REGIONAL, 10);
        CompanyDetailResponseDto company = new CompanyDetailResponseDto(UUID.randomUUID(), "테스트 업체", 100);

        given(hubRepository.findById(fromHubId)).willReturn(Optional.of(fromHub));
        given(stockTransferRepository.existsByFromHubIdAndStatusIn(any(), any())).willReturn(false);
        given(productFeignClient.getCompaniesByHubId(fromHubId))
                .willReturn(ApiResponse.success(List.of(company)));
        givenCandidates(fromHubId, toHub, 5);

        assertThatThrownBy(() ->
                stockTransferService.createTransfer(new StockTransferCreateRequestDto(fromHubId)))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_EXCEEDS_CAPACITY));
    }

    @Test
    @DisplayName("재고 이전 단건 조회 성공")
    void getTransfer_success() {
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result = stockTransferService.getTransfer(transfer.getId());

        assertThat(result.transferId()).isEqualTo(transfer.getId());
        assertThat(result.status()).isEqualTo(TransferStatus.PENDING);
    }

    @Test
    @DisplayName("재고 이전 단건 조회 - 이전 없음")
    void getTransfer_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.getTransfer(transferId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("재고 이전 목록 조회 성공 (MASTER - 상태 필터)")
    void getTransfers_success() {
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(transfer)));

        PageResponse<StockTransferResponseDto> result =
                stockTransferService.getTransfers(TransferStatus.PENDING, null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(TransferStatus.PENDING);
    }

    @Test
    @DisplayName("허브별 재고 이전 목록 조회 성공 (HUB_MANAGER)")
    void getTransfersByHub_success() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(transfer)));

        PageResponse<StockTransferResponseDto> result =
                stockTransferService.getTransfersByHub(null, hubId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).fromHubId()).isEqualTo(hubId);
    }

    @Test
    @DisplayName("배송 담당자별 재고 이전 목록 조회 성공 (DELIVERY_MANAGER)")
    void getTransfersByDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", deliveryManagerId);

        given(stockTransferRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(transfer)));

        PageResponse<StockTransferResponseDto> result =
                stockTransferService.getTransfersByDeliveryManager(TransferStatus.IN_PROGRESS, deliveryManagerId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).deliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("허브 단건 조회 성공 - 출발 허브")
    void getTransferByHub_successAsFromHub() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result = stockTransferService.getTransferByHub(transfer.getId(), hubId);

        assertThat(result.fromHubId()).isEqualTo(hubId);
    }

    @Test
    @DisplayName("허브 단건 조회 성공 - 도착 허브")
    void getTransferByHub_successAsToHub() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), hubId, TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result = stockTransferService.getTransferByHub(transfer.getId(), hubId);

        assertThat(result.toHubId()).isEqualTo(hubId);
    }

    @Test
    @DisplayName("허브 단건 조회 - 권한 없음 (fromHubId도 toHubId도 아님)")
    void getTransferByHub_forbidden() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.getTransferByHub(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("배송 담당자 단건 조회 성공")
    void getTransferByDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", deliveryManagerId);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result =
                stockTransferService.getTransferByDeliveryManager(transfer.getId(), deliveryManagerId);

        assertThat(result.deliveryManagerId()).isEqualTo(deliveryManagerId);
    }

    @Test
    @DisplayName("배송 담당자 단건 조회 - 권한 없음 (다른 담당자)")
    void getTransferByDeliveryManager_forbidden() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", UUID.randomUUID());

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() ->
                stockTransferService.getTransferByDeliveryManager(transfer.getId(), deliveryManagerId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("출발 처리 성공 - Kafka 이벤트 발행 검증")
    void dispatch_success() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));
        given(stockTransferRepository.countByFromHubIdAndStatus(hubId, TransferStatus.IN_PROGRESS)).willReturn(0L);

        StockTransferResponseDto result = stockTransferService.dispatch(transfer.getId(), hubId);

        assertThat(result.status()).isEqualTo(TransferStatus.IN_PROGRESS);
        verify(kafkaProducer).sendDispatched(transfer.getId(), hubId, transfer.getToHubId());
    }

    @Test
    @DisplayName("출발 처리 - 이전 없음")
    void dispatch_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.dispatch(transferId, UUID.randomUUID()))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("출발 처리 - 권한 없음 (fromHubId 불일치)")
    void dispatch_forbidden() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.dispatch(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("출발 처리 - PENDING 상태 아님 (IN_PROGRESS)")
    void dispatch_notPending() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.IN_PROGRESS);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.dispatch(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }

    @Test
    @DisplayName("출발 처리 - 출발 허브에 이미 IN_PROGRESS 이전 존재")
    void dispatch_alreadyInProgress() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));
        given(stockTransferRepository.countByFromHubIdAndStatus(hubId, TransferStatus.IN_PROGRESS)).willReturn(1L);

        assertThatThrownBy(() -> stockTransferService.dispatch(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_ALREADY_IN_PROGRESS));
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 성공")
    void completeByDeliveryManager_success() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", deliveryManagerId);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));
        given(productFeignClient.bulkHubTransfer(any())).willReturn(ApiResponse.success());

        StockTransferResponseDto result = stockTransferService.completeByDeliveryManager(
                transfer.getId(), deliveryManagerId, new StockTransferCompleteRequestDto("완료 메모"));

        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.note()).isEqualTo("완료 메모");
        verify(productFeignClient).bulkHubTransfer(any());
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 - 이전 없음")
    void completeByDeliveryManager_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                stockTransferService.completeByDeliveryManager(transferId, UUID.randomUUID(), null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 - 담당자 미배정 (deliveryManagerId null)")
    void completeByDeliveryManager_deliveryManagerNotAssigned() {
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        // deliveryManagerId is null by default

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() ->
                stockTransferService.completeByDeliveryManager(transfer.getId(), UUID.randomUUID(), null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 - 권한 없음 (다른 담당자)")
    void completeByDeliveryManager_forbidden() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", UUID.randomUUID());

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() ->
                stockTransferService.completeByDeliveryManager(transfer.getId(), deliveryManagerId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 - 이미 완료됨")
    void completeByDeliveryManager_alreadyCompleted() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.COMPLETED);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", deliveryManagerId);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() ->
                stockTransferService.completeByDeliveryManager(transfer.getId(), deliveryManagerId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_ALREADY_COMPLETED));
    }

    @Test
    @DisplayName("배송 담당자 완료 처리 - IN_PROGRESS 아님 (PENDING)")
    void completeByDeliveryManager_notInProgress() {
        UUID deliveryManagerId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);
        ReflectionTestUtils.setField(transfer, "deliveryManagerId", deliveryManagerId);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() ->
                stockTransferService.completeByDeliveryManager(transfer.getId(), deliveryManagerId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }

    @Test
    @DisplayName("허브 관리자 완료 처리 성공")
    void complete_success() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), hubId, TransferStatus.IN_PROGRESS);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));
        given(productFeignClient.bulkHubTransfer(any())).willReturn(ApiResponse.success());

        StockTransferResponseDto result = stockTransferService.complete(
                transfer.getId(), hubId, new StockTransferCompleteRequestDto("입고 완료"));

        assertThat(result.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.note()).isEqualTo("입고 완료");
        verify(productFeignClient).bulkHubTransfer(any());
    }

    @Test
    @DisplayName("허브 관리자 완료 처리 - 이전 없음")
    void complete_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.complete(transferId, UUID.randomUUID(), null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("허브 관리자 완료 처리 - 권한 없음 (toHubId 불일치)")
    void complete_forbidden() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.complete(transfer.getId(), hubId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("허브 관리자 완료 처리 - 이미 완료됨")
    void complete_alreadyCompleted() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), hubId, TransferStatus.COMPLETED);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.complete(transfer.getId(), hubId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_ALREADY_COMPLETED));
    }

    @Test
    @DisplayName("허브 관리자 완료 처리 - IN_PROGRESS 아님 (PENDING)")
    void complete_notInProgress() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), hubId, TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.complete(transfer.getId(), hubId, null))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }

    @Test
    @DisplayName("재고 이전 취소 성공 (MASTER)")
    void cancel_master_success() {
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result = stockTransferService.cancel(transfer.getId());

        assertThat(result.status()).isEqualTo(TransferStatus.CANCELLED);
    }

    @Test
    @DisplayName("재고 이전 취소 - 이전 없음 (MASTER)")
    void cancel_master_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.cancel(transferId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("재고 이전 취소 - PENDING 아님 (MASTER)")
    void cancel_master_notPending() {
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.IN_PROGRESS);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.cancel(transfer.getId()))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }

    @Test
    @DisplayName("재고 이전 취소 성공 (HUB_MANAGER)")
    void cancel_hubManager_success() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        StockTransferResponseDto result = stockTransferService.cancel(transfer.getId(), hubId);

        assertThat(result.status()).isEqualTo(TransferStatus.CANCELLED);
    }

    @Test
    @DisplayName("재고 이전 취소 - 이전 없음 (HUB_MANAGER)")
    void cancel_hubManager_notFound() {
        UUID transferId = UUID.randomUUID();
        given(stockTransferRepository.findById(transferId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stockTransferService.cancel(transferId, UUID.randomUUID()))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_NOT_FOUND));
    }

    @Test
    @DisplayName("재고 이전 취소 - 권한 없음 (HUB_MANAGER, fromHubId 불일치)")
    void cancel_hubManager_forbidden() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(UUID.randomUUID(), UUID.randomUUID(), TransferStatus.PENDING);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.cancel(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("재고 이전 취소 - PENDING 아님 (HUB_MANAGER)")
    void cancel_hubManager_notPending() {
        UUID hubId = UUID.randomUUID();
        StockTransfer transfer = buildTransfer(hubId, UUID.randomUUID(), TransferStatus.IN_PROGRESS);

        given(stockTransferRepository.findById(transfer.getId())).willReturn(Optional.of(transfer));

        assertThatThrownBy(() -> stockTransferService.cancel(transfer.getId(), hubId))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                        .isEqualTo(HubErrorCode.TRANSFER_INVALID_STATUS));
    }
}
