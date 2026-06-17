package com.example.resumematcher

import kotlin.math.sqrt

data class MatchResult(
    val score: Int,
    val matchedKeywords: List<String>,
    val missingKeywords: List<String>,
    val resumeWordCount: Int,
    val jdWordCount: Int
)

object MatcherEngine {

    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "is", "are", "was", "were", "of", "to", "in", 
        "for", "with", "on", "at", "by", "from", "as", "it", "this", "that", "these", "those", 
        "i", "you", "he", "she", "they", "we", "us", "them", "my", "your", "his", "her", 
        "their", "our", "its", "me", "him", "be", "been", "have", "has", "had", "do", "does", 
        "did", "will", "would", "shall", "should", "can", "could", "may", "might", "must", 
        "about", "above", "after", "again", "against", "all", "am", "any", "because", 
        "before", "being", "below", "between", "both", "during", "each", "few", "further", 
        "here", "there", "when", "where", "why", "how", "up", "down", "out", "over", "under", 
        "then", "once", "more", "most", "other", "some", "such", "no", "nor", "not", "only", 
        "own", "same", "so", "than", "too", "very", "s", "t", "just", "don", "now", "an", "the"
    )

    private val genericWords = setOf(
        "experience", "work", "job", "description", "resume", "team", "development", "project", 
        "projects", "working", "years", "candidate", "role", "position", "ability", "skills", 
        "requirements", "responsibilities", "application", "support", "using", "design", "develop", 
        "implement", "building", "systems", "solutions", "software", "product", "technology", 
        "technologies", "business", "knowledge", "required", "preferred", "duties", "qualifications", 
        "education", "degree", "field", "related", "equivalent", "etc", "e.g.", "i.e.", "one", 
        "two", "three", "first", "second", "third", "highly", "strong", "excellent", "good", 
        "best", "great", "successful", "environment", "tools", "processes", "tasks", "duties", 
        "needs", "responsibilities", "performing", "perform", "etc."
    )

    fun calculateMatch(resumeText: String, jdText: String): MatchResult {
        // Tokenize and clean
        val resumeTokens = tokenize(resumeText)
        val jdTokens = tokenize(jdText)

        val resumeWordCount = resumeTokens.size
        val jdWordCount = jdTokens.size

        if (resumeTokens.isEmpty() || jdTokens.isEmpty()) {
            return MatchResult(0, emptyList(), emptyList(), resumeWordCount, jdWordCount)
        }

        // 1. Calculate Cosine Similarity
        val resumeFreqs = resumeTokens.groupingBy { it }.eachCount()
        val jdFreqs = jdTokens.groupingBy { it }.eachCount()

        var dotProduct = 0.0
        for ((word, count) in jdFreqs) {
            val rCount = resumeFreqs[word] ?: 0
            dotProduct += count * rCount
        }

        val magResume = sqrt(resumeFreqs.values.sumOf { it * it }.toDouble())
        val magJd = sqrt(jdFreqs.values.sumOf { it * it }.toDouble())

        val cosineSimilarity = if (magResume > 0.0 && magJd > 0.0) {
            dotProduct / (magResume * magJd)
        } else {
            0.0
        }

        // 2. Extract Keywords (filter out stop words and generic words)
        val jdTargetTokens = jdTokens.filter { it !in genericWords && it.length >= 3 }
        val jdTargetFreqs = jdTargetTokens.groupingBy { it }.eachCount()
        
        // Sort target keywords by frequency in Job Description
        val sortedKeywords = jdTargetFreqs.entries
            .sortedByDescending { it.value }
            .map { it.key }

        // Take top 15 keywords
        val topKeywords = sortedKeywords.take(15)

        val resumeTokenSet = resumeTokens.toSet()
        val matchedKeywords = mutableListOf<String>()
        val missingKeywords = mutableListOf<String>()

        for (kw in topKeywords) {
            if (resumeTokenSet.contains(kw)) {
                matchedKeywords.add(kw)
            } else {
                missingKeywords.add(kw)
            }
        }

        val keywordMatchScore = if (topKeywords.isNotEmpty()) {
            (matchedKeywords.size.toDouble() / topKeywords.size.toDouble())
        } else {
            1.0
        }

        // Combined Score: 40% Cosine Similarity + 60% Keyword Coverage
        val combinedScore = (cosineSimilarity * 0.4 + keywordMatchScore * 0.6) * 100
        val finalScore = combinedScore.coerceIn(0.0, 100.0).toInt()

        return MatchResult(
            score = finalScore,
            matchedKeywords = matchedKeywords,
            missingKeywords = missingKeywords,
            resumeWordCount = resumeWordCount,
            jdWordCount = jdWordCount
        )
    }

    private fun tokenize(text: String): List<String> {
        // Clean text: keep letters, numbers, and symbols common in tech (+ for C++, # for C#, . for .NET/Node.js, - for React-Native)
        val cleaned = text.lowercase().replace(Regex("[^a-z0-9+#.\\-]"), " ")
        return cleaned.split(Regex("\\s+"))
            .filter { it.isNotEmpty() && it !in stopWords }
    }
}
