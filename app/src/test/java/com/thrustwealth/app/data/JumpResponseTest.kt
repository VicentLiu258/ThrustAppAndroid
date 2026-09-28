package com.thrustwealth.app.data

import com.thrustwealth.app.data.model.JumpSearchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JumpResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun testParseSuccessJumpResponse() {
        val rawJson = """{"code":1,"msg":"ok","data":{"url":"https:\/\/ic.solutionsace.com\/","resident":1}}"""
        val response = json.decodeFromString<JumpSearchResponse>(rawJson)

        assertEquals(1, response.code)
        assertEquals("ok", response.msg)
        assertNotNull(response.data)
        assertEquals("https://ic.solutionsace.com/", response.data?.url)
        assertEquals(1, response.data?.resident)
    }

    @Test
    fun testParseNoMatchJumpResponse() {
        val rawJson = """{"code":0,"msg":"未匹配到跳转规则","data":null}"""
        val response = json.decodeFromString<JumpSearchResponse>(rawJson)

        assertEquals(0, response.code)
        assertEquals("未匹配到跳转规则", response.msg)
        assertNull(response.data)
    }
}
