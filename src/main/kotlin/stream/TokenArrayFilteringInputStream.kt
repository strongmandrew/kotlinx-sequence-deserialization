package stream

import java.io.FilterInputStream
import java.io.InputStream
import java.util.Stack
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Производит фильтрацию входящего потока байт до вхождения [token] и до окончания массива JSON, который
 * [token] начинает
 */
class TokenArrayFilteringInputStream(
    private val inputStream: InputStream,
    private val token: String
) : FilterInputStream(inputStream) {

    init {
        require(token.trim().asSequence().none(Char::isWhitespace)) {
            "Для точного поиска входящий токен не должен содержать внутри себя пробелов и пр. символов `whitespace`. Был предоставлен $token"
        }
    }

    companion object {
        const val START_OF_JSON_ARRAY = '['
        const val END_OF_JSON_ARRAY = ']'

        private val specialChars = ('a'..'z').plus("\"")
    }

    private val arrayStarted = AtomicBoolean(false)
    private val isEndOfArray = AtomicBoolean(false)
    private val bracesStack = Stack<Char>()

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (isEndOfArray.get())
            return -1

        return inputStream.markReset(len) {

            val parsedString = readString(off, len).shiftEnding()

            if (arrayStarted.get()) {
                return@markReset 0 to parsedString.searchLastBraceIndex()
            }

            if (token in parsedString) {

                arrayStarted.set(true)

                val skippedStartOfValue = parsedString.lastIndexOf(token) + token.length

                return@markReset skippedStartOfValue to parsedString.drop(skippedStartOfValue).searchLastBraceIndex()

            } else {
                return this@TokenArrayFilteringInputStream.read(b, off, len)
            }

        }.let { (skippedAmount, updatedLength) ->
            inputStream.skip(skippedAmount.toLong())

            inputStream.read(b, off, updatedLength)
        }
    }

    /**
     * Возвращает обрезанную строку, чтобы она не заканчивалась на [specialChars].
     * Это упрощает поиск [token], так как строка не будет заканчиваться на полуслове
     */
    private fun String.shiftEnding(): String = asSequence().indexOfLast { char ->
        char !in specialChars
    }.let { take(it) }

    /**
     * Накапливает символы начала и конца JSON массива в стеке. Если извлекается последняя квадратная скобка из стека,
     * то это признак окончания массива: в этом случае возвращается индекс следующий после квадратной скобки, иначе длина исходной строки.
     * Именно так определяется, нужно ли дальше читать байты из потока, пока массив ещё не закончен.
     */
    private fun String.searchLastBraceIndex(): Int {
        forEachIndexed { index, nextValue ->

            when (nextValue) {
                START_OF_JSON_ARRAY -> bracesStack.push(nextValue)
                END_OF_JSON_ARRAY -> {
                    bracesStack.pop()

                    if (bracesStack.empty()) {
                        isEndOfArray.set(true)
                        return index + 1
                    }
                }
            }
        }

        return length
    }

    private inline fun <T> InputStream.markReset(len: Int, action: InputStream.() -> T): T {
        require(this.markSupported()) {
            "Операция mark/reset не поддерживается текущей реализацией `InputStream`. Выберите другой вариант"
        }

        this.mark(len)

        return this.action().also { this.reset() }
    }

    private fun InputStream.readString(offset: Int, len: Int): String {
        val tempBytes = ByteArray(len - offset)

        read(tempBytes, offset, len)

        return String(tempBytes)
    }
}