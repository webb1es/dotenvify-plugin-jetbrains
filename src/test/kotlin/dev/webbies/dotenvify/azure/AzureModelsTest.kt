package dev.webbies.dotenvify.azure

import org.junit.Assert.*
import org.junit.Test

class AzureModelsTest {

    @Test
    fun `parseUrl with dev azure com format`() {
        val (org, project) = AzureConnection.parseUrl("https://dev.azure.com/myorg/myproject")
        assertEquals("myorg", org)
        assertEquals("myproject", project)
    }

    @Test
    fun `parseUrl with dev azure com trailing slash`() {
        val (org, project) = AzureConnection.parseUrl("https://dev.azure.com/myorg/myproject/")
        assertEquals("myorg", org)
        assertEquals("myproject", project)
    }

    @Test
    fun `parseUrl with visualstudio com format`() {
        val (org, project) = AzureConnection.parseUrl("https://myorg.visualstudio.com/myproject")
        assertEquals("myorg", org)
        assertEquals("myproject", project)
    }

    @Test
    fun `parseUrl with visualstudio com trailing slash`() {
        val (org, project) = AzureConnection.parseUrl("https://myorg.visualstudio.com/myproject/")
        assertEquals("myorg", org)
        assertEquals("myproject", project)
    }

    @Test
    fun `parseUrl with http protocol`() {
        val (org, project) = AzureConnection.parseUrl("http://dev.azure.com/testorg/testproject")
        assertEquals("testorg", org)
        assertEquals("testproject", project)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseUrl with invalid URL`() {
        AzureConnection.parseUrl("https://example.com/not/azure")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseUrl with incomplete URL`() {
        AzureConnection.parseUrl("https://dev.azure.com/myorg")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseUrl with empty URL`() {
        AzureConnection.parseUrl("")
    }

    @Test
    fun `AzureConnection organization property`() {
        val conn = AzureConnection("https://dev.azure.com/myorg/myproject", "myproject")
        assertEquals("myorg", conn.organization)
    }

    @Test
    fun `VariableGroup data class`() {
        val group = VariableGroup(
            id = 1,
            name = "test-group",
            variables = mapOf("KEY" to Variable("value", false)),
        )
        assertEquals(1, group.id)
        assertEquals("test-group", group.name)
        assertEquals("value", group.variables["KEY"]?.value)
    }
}
