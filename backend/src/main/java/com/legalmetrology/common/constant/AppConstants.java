package com.legalmetrology.common.constant;

public final class AppConstants {

    private AppConstants() {
    }

    public static final String API_BASE_PATH = "/api/v1";

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";
    public static final String DEFAULT_SORT_FIELD = "createdAt";
    public static final int MAX_PAGE_SIZE = 100;

    public static final String ROLE_PREFIX = "ROLE_";
}
