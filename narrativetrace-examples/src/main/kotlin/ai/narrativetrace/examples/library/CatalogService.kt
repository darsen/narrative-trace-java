package ai.narrativetrace.examples.library

import ai.narrativetrace.core.annotation.OnError

interface CatalogService {
    @OnError(value = "Book {isbn} not found in catalog", exception = BookNotFoundException::class)
    fun findBook(isbn: String): Book
}

class InMemoryCatalogService : CatalogService {

    private val books = mapOf(
        "978-0-13-468599-1" to Book("978-0-13-468599-1", "The Pragmatic Programmer", "David Thomas & Andrew Hunt", true),
        "978-0-201-63361-0" to Book("978-0-201-63361-0", "Design Patterns", "Gang of Four", true),
        "978-0-13-235088-4" to Book("978-0-13-235088-4", "Clean Code", "Robert C. Martin", false)
    )

    override fun findBook(isbn: String): Book =
        books[isbn] ?: throw BookNotFoundException(isbn)
}
