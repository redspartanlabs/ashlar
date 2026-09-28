package com.redspartan.ashlar.icon;

/**
 * Identifies a supported icon. This is purely an identity - it carries no
 * markup. See {@link IconSvg} for the actual SVG each value renders as.
 *
 * <p>Grouped by rough category purely for readability here; nothing about
 * the grouping is meaningful to callers or to {@link IconSvg}.
 */
public enum Icon {
    // Original set
    CALENDAR,
    CHEVRON_LEFT,
    CHEVRON_RIGHT,
    CHEVRON_DOWN,
    UPLOAD,
    SEARCH,
    EDIT,
    DELETE,
    SAVE,
    PLUS,
    CHECK,
    CLOSE,
    EYE,
    FILTER,
    USER,
    SETTINGS,
    SHIELD,
    SUN,
    MOON,
    CHECK_CIRCLE,
    INFO_CIRCLE,
    WARNING_TRIANGLE,
    X_CIRCLE,
    BELL,

    // Navigation
    CHEVRON_UP,
    ARROW_LEFT,
    ARROW_RIGHT,
    ARROW_UP,
    ARROW_DOWN,
    MENU,
    MORE_HORIZONTAL,
    MORE_VERTICAL,
    EXTERNAL_LINK,
    HOME,

    // Actions
    DOWNLOAD,
    COPY,
    DUPLICATE,
    REFRESH,
    PRINT,
    SHARE,
    SEND,
    ARCHIVE,
    LOCK,
    UNLOCK,
    PIN,
    LINK,

    // Files & documents
    FILE,
    FOLDER,
    FOLDER_OPEN,
    FILE_TEXT,
    CLIPBOARD,
    PAPERCLIP,
    IMAGE,
    BOOK,

    // Communication
    MAIL,
    PHONE,
    MESSAGE,
    INBOX,
    VIDEO,

    // Users & organization
    USERS,
    USER_PLUS,
    USER_CHECK,
    USER_X,
    BUILDING,
    BRIEFCASE,
    ID_BADGE,

    // Data & analytics
    CHART_BAR,
    CHART_LINE,
    CHART_PIE,
    TRENDING_UP,
    TRENDING_DOWN,
    DATABASE,
    SERVER,
    ACTIVITY,

    // Commerce
    SHOPPING_CART,
    CREDIT_CARD,
    DOLLAR_SIGN,
    TAG,
    RECEIPT,
    WALLET,

    // Status & feedback
    HELP_CIRCLE,
    ALERT_CIRCLE,
    STAR,
    FLAG,
    THUMBS_UP,
    THUMBS_DOWN,

    // Layout
    GRID,
    LIST,
    LAYOUT,
    COLUMNS,
    MAXIMIZE,
    MINIMIZE,
    SLIDERS,

    // Time
    CLOCK,

    // Security & session
    KEY,
    LOG_OUT,
    LOG_IN,

    // Misc
    GLOBE,
    MAP_PIN,
    CLOUD
}
