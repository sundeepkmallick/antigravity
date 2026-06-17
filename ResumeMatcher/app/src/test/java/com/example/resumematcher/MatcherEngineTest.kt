package com.example.resumematcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatcherEngineTest {

    @Test
    fun testCalculateMatch_withMatchingKeywords() {
        val resumeText = "Experienced Android developer skilled in Kotlin, Java, Jetpack Compose, and Git."
        val jdText = "We are looking for an Android developer with strong knowledge of Kotlin, Java, and Git."

        val result = MatcherEngine.calculateMatch(resumeText, jdText)

        // The keywords we expect to extract from the JD (excluding stop words and generic words like "looking", "knowledge", etc.):
        // "android", "kotlin", "java", "git"
        assertTrue("Android should be in matched keywords", result.matchedKeywords.contains("android"))
        assertTrue("Kotlin should be in matched keywords", result.matchedKeywords.contains("kotlin"))
        assertTrue("Java should be in matched keywords", result.matchedKeywords.contains("java"))
        assertTrue("Git should be in matched keywords", result.matchedKeywords.contains("git"))
        assertTrue("No missing keywords expected", result.missingKeywords.isEmpty())
        assertTrue("Score should be high", result.score >= 80)
    }

    @Test
    fun testCalculateMatch_withMissingKeywords() {
        val resumeText = "Web developer with HTML, CSS, JavaScript, and React experience."
        val jdText = "Android Developer required. Must have Kotlin, Java, and Compose skills."

        val result = MatcherEngine.calculateMatch(resumeText, jdText)

        // Expected missing keywords from JD: "android", "kotlin", "java", "compose"
        assertTrue("Android should be missing", result.missingKeywords.contains("android"))
        assertTrue("Kotlin should be missing", result.missingKeywords.contains("kotlin"))
        assertTrue("Java should be missing", result.missingKeywords.contains("java"))
        assertTrue("Compose should be missing", result.missingKeywords.contains("compose"))
        assertTrue("Score should be low", result.score < 20)
    }

    @Test
    fun testCalculateMatch_emptyInputs() {
        val result = MatcherEngine.calculateMatch("", "")
        assertEquals(0, result.score)
        assertTrue(result.matchedKeywords.isEmpty())
        assertTrue(result.missingKeywords.isEmpty())
    }

    @Test
    fun testCleanAndTokenizeSpecialTechSymbols() {
        val resumeText = "Expert in C++, C#, .NET, Node.js, and React-Native."
        val jdText = "Requires experience with C++, C#, .NET, Node.js, and React-Native."

        val result = MatcherEngine.calculateMatch(resumeText, jdText)

        // Make sure specialized programming terms with symbols are matched exactly
        assertTrue(result.matchedKeywords.contains("c++"))
        assertTrue(result.matchedKeywords.contains("c#"))
        assertTrue(result.matchedKeywords.contains(".net"))
        assertTrue(result.matchedKeywords.contains("node.js"))
        assertTrue(result.matchedKeywords.contains("react-native"))
    }
}
