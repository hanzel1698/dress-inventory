package com.hanzel.dressinventory.data

private val CODE_PATTERN = Regex("^([A-Z]+)([1-9])$")
private const val GROUP_SIZE = 9

/** Convert a global wardrobe index to a code (0 → A1, 9 → B1, 18 → C1, …). */
fun dressCodeFromIndex(index: Int): String {
    require(index >= 0) { "index must be non-negative" }
    val group = index / GROUP_SIZE
    val digit = (index % GROUP_SIZE) + 1
    return "${groupLetters(group)}$digit"
}

/** Parse a wardrobe code back to its global index, or null if invalid. */
fun dressCodeToIndex(code: String): Int? {
    val match = CODE_PATTERN.matchEntire(code.trim()) ?: return null
    val group = lettersToGroup(match.groupValues[1]) ?: return null
    val digit = match.groupValues[2].toInt()
    return group * GROUP_SIZE + (digit - 1)
}

private fun groupLetters(group: Int): String {
    var n = group
    val chars = StringBuilder()
    while (true) {
        chars.insert(0, 'A' + (n % 26))
        n = n / 26 - 1
        if (n < 0) break
    }
    return chars.toString()
}

private fun lettersToGroup(letters: String): Int? {
    if (letters.isEmpty()) return null
    var group = 0
    for (ch in letters) {
        if (ch !in 'A'..'Z') return null
        group = group * 26 + (ch - 'A' + 1)
    }
    return group - 1
}

private fun usedDressCodeIndices(dresses: List<Dress>): Set<Int> =
    dresses.mapNotNull { dressCodeToIndex(it.code) }.toSet()

/** Next free ID in the global sequence (A1–A9, B1–B9, C1–C9, …). */
fun nextDressCode(dresses: List<Dress>): String {
    val used = usedDressCodeIndices(dresses)
    var index = 0
    while (index in used) index++
    return dressCodeFromIndex(index)
}

/** Backfill IDs for existing wardrobe pieces that do not have one yet. */
fun assignMissingDressCodes(dresses: List<Dress>): List<Dress> {
    val result = dresses.toMutableList()
    for (i in result.indices) {
        if (result[i].code.isBlank() || dressCodeToIndex(result[i].code) == null) {
            val code = nextDressCode(result)
            result[i] = result[i].copy(code = code)
        }
    }
    return result
}

/** Assign or preserve an ID when saving a piece. */
fun ensureDressCode(dresses: List<Dress>, dress: Dress): Dress {
    if (dress.code.isNotBlank() && dressCodeToIndex(dress.code) != null) return dress
    return dress.copy(code = nextDressCode(dresses.filter { it.id != dress.id }))
}
