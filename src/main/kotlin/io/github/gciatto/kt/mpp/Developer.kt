package io.github.gciatto.kt.mpp

import io.github.gciatto.kt.node.People

data class Developer(
    val name: String,
    val email: String,
    val homepage: String? = null,
    val organization: Organization? = null
) {
    fun toNpmPeople() = People(name, email, homepage)
}
