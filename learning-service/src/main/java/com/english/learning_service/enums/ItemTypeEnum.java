package com.english.learning_service.enums;

public enum ItemTypeEnum {
    VOCABULARY,
    GRAMMAR,
    LISTENING,
    FULL_TEST; // use for full test like TOEIC test

    public static ItemTypeEnum map(FilterType filterType) {
        return switch (filterType) {
            case VOCABULARY -> VOCABULARY;
            case GRAMMAR -> GRAMMAR;
            case LISTENING -> LISTENING;
            case FULL_TEST -> FULL_TEST;
            default -> null; // or throw an exception if ALL doesn't make sense
        };
    }
}
