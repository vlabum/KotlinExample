package ru.skillbranch.kotlinexample.extentions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    var i = 0
    var n = 0
    for(t in this) {
        if (predicate(t)) {
            n = i
        }
        i++
    }
    return dropLast(this.size - n)
}

