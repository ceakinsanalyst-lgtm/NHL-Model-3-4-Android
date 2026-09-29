package com.professorbets.nhlmodel34

object Csv {
    fun parseLine(line: String): List<String> {
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var quoted = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && quoted && i + 1 < line.length && line[i+1] == '"' -> { sb.append('"'); i++ }
                ch == '"' -> quoted = !quoted
                ch == ',' && !quoted -> { out += sb.toString(); sb.setLength(0) }
                else -> sb.append(ch)
            }
            i++
        }
        out += sb.toString()
        return out
    }
}
