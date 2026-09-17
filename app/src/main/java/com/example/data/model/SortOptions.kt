package com.example.data.model

enum class SortType(val label: String) {
    NAME("Name"),
    SIZE("Size"),
    DATE("Date Modified"),
    TYPE("File Type");

    val title: String get() = label
}

enum class SortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

enum class ViewMode {
    LIST,
    GRID
}

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}
