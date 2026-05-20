package com.boxoffice.common.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 조회 결과의 공통 응답 래퍼 클래스.
 * Spring Data의 Page 객체를 클라이언트 응답 형식으로 변환한다.
 *
 * 사용 예시:
 *   Page<HubResponse> page = hubService.getHubs(pageable);
 *   return ResponseEntity.ok(ApiResponse.success(PageResponse.of(page)));
 */
@Getter
public class PageResponse<T> {

    /** 현재 페이지 데이터 목록 */
    private final List<T> content;

    /** 현재 페이지 번호 (0부터 시작) */
    private final int page;

    /** 현재 페이지 크기 */
    private final int size;

    /** 전체 데이터 수 */
    private final long totalElements;

    /** 전체 페이지 수 */
    private final int totalPages;

    /** 정렬 기준 (예: "createdAt,DESC") */
    private final String sort;

    private PageResponse(Page<T> page, String sort) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.sort = sort;
    }

    /**
     * Page 객체를 PageResponse로 변환한다.
     *
     * @param page Spring Data Page 객체
     * @param sort 정렬 기준 문자열 (예: "createdAt,DESC")
     * @return 페이징 응답 객체
     */
    public static <T> PageResponse<T> of(Page<T> page, String sort) {
        return new PageResponse<>(page, sort);
    }

    /**
     * Page 객체를 PageResponse로 변환한다 (기본 정렬: createdAt,DESC).
     *
     * @param page Spring Data Page 객체
     * @return 페이징 응답 객체
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page, "createdAt,DESC");
    }
}