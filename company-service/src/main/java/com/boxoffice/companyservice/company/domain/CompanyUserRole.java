package com.boxoffice.companyservice.company.domain;

import java.util.Locale;

public enum CompanyUserRole {
    MASTER,
    HUB_MANAGER,
    DELIVERY_MANAGER,
    SUPPLIER_MANAGER,
    UNKNOWN;

    public static CompanyUserRole fromString(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return UNKNOWN;
        }

        try {
            return CompanyUserRole.valueOf(roleStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
