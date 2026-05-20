package com.boxoffice.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * 페이징 파라미터 검증 유틸리티 클래스.
 * 허용된 페이지 크기(10, 30, 50)만 사용하도록 강제하며,
 * 잘못된 값이 들어오면 기본값(10)으로 대체한다.
 */
public class PageableUtils {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);

    private PageableUtils() {}

    /**
     * 페이징 파라미터를 검증하고 Pageable 객체를 반환한다.
     *
     * @param page      페이지 번호 (0부터 시작, 음수면 0으로 보정)
     * @param size      페이지 크기 (10, 30, 50만 허용. 그 외는 10으로 고정)
     * @param sortField 정렬 기준 필드명 (예: "createdAt")
     * @param desc      true면 내림차순, false면 오름차순
     * @return 검증된 Pageable 객체
     */
    public static Pageable of(int page, int size, String sortField, boolean desc) {
        int validSize = ALLOWED_SIZES.contains(size) ? size : DEFAULT_SIZE;
        int validPage = Math.max(page, DEFAULT_PAGE);
        Sort sort = desc
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();
        return PageRequest.of(validPage, validSize, sort);
    }

    /**
     * 기본 정렬(createdAt 내림차순)이 적용된 Pageable 객체를 반환한다.
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (10, 30, 50만 허용)
     * @return 기본 정렬이 적용된 Pageable 객체
     */
    public static Pageable ofDefault(int page, int size) {
        return of(page, size, "createdAt", true);
    }
}