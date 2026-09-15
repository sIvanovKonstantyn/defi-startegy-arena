package com.defistrategyarena.strategy.application;

public record ListStrategiesQuery(
        String ownerId, int page, int size, String sort, String order) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 100;
    public static final String SORT_NAME = "name";
    public static final String SORT_STRATEGY_ID = "strategyId";
    public static final String ORDER_ASC = "asc";
    public static final String ORDER_DESC = "desc";

    private static final String OWNER_REQUIRED = "owner id must not be blank";
    private static final String PAGE_INVALID = "page must be zero or greater";
    private static final String SIZE_INVALID = "size must be between 1 and 100";
    private static final String SORT_INVALID = "sort must be name or strategyId";
    private static final String ORDER_INVALID = "order must be asc or desc";

    public ListStrategiesQuery {
        if (ownerId == null || ownerId.isBlank()) {
            throw new IllegalArgumentException(OWNER_REQUIRED);
        }
        if (page < DEFAULT_PAGE) {
            throw new IllegalArgumentException(PAGE_INVALID);
        }
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException(SIZE_INVALID);
        }
        String normalizedSort = sort == null ? SORT_NAME : sort;
        String normalizedOrder = order == null ? ORDER_ASC : order;
        if (!SORT_NAME.equals(normalizedSort) && !SORT_STRATEGY_ID.equals(normalizedSort)) {
            throw new IllegalArgumentException(SORT_INVALID);
        }
        if (!ORDER_ASC.equals(normalizedOrder) && !ORDER_DESC.equals(normalizedOrder)) {
            throw new IllegalArgumentException(ORDER_INVALID);
        }
        sort = normalizedSort;
        order = normalizedOrder;
    }

    public static ListStrategiesQuery create(ListStrategiesQuery draft) {
        return new ListStrategiesQuery(
                draft.ownerId(), draft.page(), draft.size(), draft.sort(), draft.order());
    }
}
