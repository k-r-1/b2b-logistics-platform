package com.boxoffice.delivery_manager_service.common.context;

public class UserContextHolder {
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    public static void setUsername(String username) { USERNAME_HOLDER.set(username); }
    public static String getUsername() { return USERNAME_HOLDER.get(); }

    public static void setRole(String role) { ROLE_HOLDER.set(role); }
    public static String getRole() { return ROLE_HOLDER.get(); }

    public static void clear() {
        USERNAME_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}