package com.warungsync.app.domain.model

enum class MemberRole {
    OWNER,
    ADMIN,
    USER;

    companion object {
        fun fromString(value: String): MemberRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: USER
        }
    }
}
