package com.zerowasteeats.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApiErrorTest {

    @Test
    fun readsTheBackendsErrorBody() {
        val error = parseApiError(
            """{"message":"Se requiere un token de acceso válido.","error":"No autorizado","statusCode":401}"""
        )

        assertEquals(ApiError(401, "Se requiere un token de acceso válido."), error)
    }

    @Test
    fun takesTheFirstMessageWhenSeveralValidationsFail() {
        val error = parseApiError(
            """{"message":["El nombre debe ser texto.","La ciudad debe ser texto."],"statusCode":400}"""
        )

        assertEquals(ApiError(400, "El nombre debe ser texto."), error)
    }

    @Test
    fun ignoresFieldsItDoesNotKnow() {
        val error = parseApiError("""{"message":"Conflicto","statusCode":409,"path":"/x"}""")

        assertEquals(ApiError(409, "Conflicto"), error)
    }

    @Test
    fun returnsNullForABodyThatIsNotAnErrorObject() {
        assertNull(parseApiError("<html>Bad gateway</html>"))
        assertNull(parseApiError(""))
        assertNull(parseApiError("[1,2]"))
    }
}
