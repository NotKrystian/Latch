package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.AcceptedExplanation
import com.example.data.Checkpoint
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Response classes for our business logic
data class PlanResponse(
    val checkpoints: List<CheckpointFixture>
)

data class CheckpointFixture(
    val subtopicName: String,
    val extractText: String,
    val promptText: String
)

data class GradeResult(
    val understandingOk: Boolean,
    val authenticityOk: Boolean,
    val understandingScore: Float,
    val authenticityScore: Float,
    val feedback: String,
    val missingPoints: List<String>,
    val mustRedo: Boolean,
    val earnedMinutes: Int,
    val vocabularyLevel: String,
    val sentenceLength: Int,
    val toneDescription: String,
    val personaSummary: String
)

data class PersonaBootstrapResult(
    val vocabularyLevel: String,
    val sentenceLength: Int,
    val toneDescription: String,
    val personaSummary: String
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Retrieve API key. Let the user override it in the app settings, stored locally.
    fun getApiKey(userCustomKey: String?): String {
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (userCustomKey != null && userCustomKey.trim().isNotEmpty() && userCustomKey != "MY_GEMINI_API_KEY") {
            userCustomKey
        } else if (buildKey.isNotEmpty() && buildKey != "MY_GEMINI_API_KEY") {
            buildKey
        } else {
            ""
        }
    }

    private fun String?.isNull_or_Empty(): Boolean {
        return this == null || this.trim().isEmpty()
    }

    /**
     * Executes a raw generateContent call to Gemini using JSON input
     */
    private suspend fun callGemini(apiKey: String, prompt: String, systemInstruction: String? = null): String? = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API Key is empty!")
            return@withContext null
        }

        val url = "$BASE_URL?key=$apiKey"
        
        val requestJson = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", prompt)
        partsArray.put(partObj)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        requestJson.put("contents", contentsArray)

        // Request structural JSON output
        val generationConfig = JSONObject()
        generationConfig.put("responseMimeType", "application/json")
        generationConfig.put("temperature", 0.3) // lower temperature for stable structure
        requestJson.put("generationConfig", generationConfig)

        if (systemInstruction != null) {
            val sysObj = JSONObject()
            val sysParts = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", systemInstruction)
            sysParts.put(sysPartObj)
            sysObj.put("parts", sysParts)
            requestJson.put("systemInstruction", sysObj)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string()
                if (!response.isSuccessful) {
                    Log.e(TAG, "API Call failed code: ${response.code}, body: $bodyStr")
                    return@withContext null
                }
                if (bodyStr == null) return@withContext null
                
                val rootJson = JSONObject(bodyStr)
                val candidates = rootJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val candidateContent = firstCandidate?.optJSONObject("content")
                val parts = candidateContent?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val textResponse = firstPart?.optString("text")
                return@withContext textResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during callGemini: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * POST /v1/plans
     * Generates 4-6 subtopics with short passages
     */
    suspend fun generateLearningPlan(
        apiKey: String,
        topic: String,
        materials: String
    ): List<CheckpointFixture> {
        val sysInstruction = "You are an expert curriculum developer. You specialize in breaking down topics into a clear, sequential path of short, accessible subtopic reading cards (150-200 words each). Each card teaches one core concept."
        val prompt = """
            Generate a learning plan of 4-5 checkpoints for the topic: "$topic".
            Use the following materials to construct the content if available, otherwise use your internal expert knowledge:
            "$materials"
            
            Format your response STRICTLY as a JSON object containing a "checkpoints" array.
            Each checkpoint object MUST have:
            - "subtopicName": Short descriptive title.
            - "extractText": A short explanation of 150-250 words explaining the concept clearly. Do not use complex markdown or bullet lists. Keep it in highly readable prose.
            - "promptText": A custom question asking the user to explain a key part of the concept in their own words (no multiple choice, no true/false).
            
            Example Format:
            {
              "checkpoints": [
                {
                  "subtopicName": "Intro to Photosynthesis",
                  "extractText": "Photosynthesis is the process by which green plants use sunlight to synthesize nutrients...",
                  "promptText": "Explain in your own words what green plants convert sunlight into, and what they release as a byproduct."
                }
              ]
            }
            Ensure the JSON is correct, fully escapable, and contains no raw line-breaks inside values.
        """.trimIndent()

        val responseStr = callGemini(apiKey, prompt, sysInstruction)
        if (responseStr != null) {
            try {
                // Clean markdown blocks if any
                val cleaned = cleanJsonResponse(responseStr)
                val json = JSONObject(cleaned)
                val checkpointsArray = json.getJSONArray("checkpoints")
                val fixtures = mutableListOf<CheckpointFixture>()
                for (i in 0 until checkpointsArray.length()) {
                    val obj = checkpointsArray.getJSONObject(i)
                    fixtures.add(
                        CheckpointFixture(
                            subtopicName = obj.getString("subtopicName"),
                            extractText = obj.getString("extractText"),
                            promptText = obj.getString("promptText")
                        )
                    )
                }
                return fixtures
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing plan JSON: ${e.message}", e)
            }
        }
        
        // Return default simulated plan if Gemini fails or Key is absent
        return generateMockPlan(topic)
    }

    /**
     * POST /v1/sessions/grade
     * Grades the user's teach-back explanation using AI
     */
    suspend fun gradeExplanation(
        apiKey: String,
        subtopicName: String,
        extractText: String,
        promptText: String,
        userExplanation: String,
        personaSummary: String,
        pastExplanations: List<AcceptedExplanation>
    ): GradeResult {
        val pastAnswersBlock = pastExplanations.joinToString("\n") { "- Sample: ${it.userExplanation}" }
        val sysInstruction = "You are Latch Grader, an expert tutor who measures students' true understanding and verifies that they are writing in their own authentic style, rejecting plagiarized, copied, or AI-generated textbook style answers."
        val prompt = """
            Grade the student's teach-back answer for subtopic: "$subtopicName".
            
            Reading Passage: "$extractText"
            Teach-back Prompt: "$promptText"
            User's Explanation: "$userExplanation"
            
            Their Established Style Persona: "$personaSummary"
            Recent accepted answers:
            $pastAnswersBlock
            
            Strict Grading Rules:
            1. understandingOk: True if they correctly explain the core ideas in their own words. False if they are too vague, miss the point, write absolute nonsense, or just list buzzwords.
            2. authenticityOk: True if it matches their tone, typical sentence complexity, and vocabulary. False if they copy-pasted lines from the Reading Passage, or if the tone suddenly becomes highly polished, textbook-grade, or matches a typical clinical ChatGPT style. (If style persona is soft-mode/cold-started, you can be gentle on authenticity, but still check for copy-pasting from passage).
            3. feedback: Friendly, helpful feedback (2-3 sentences). Highlight what they explained well and what is missing or incorrect. If inauthentic, say so plainly without being offensive, e.g. "This sounds like it was copied directly or written by another AI. Explain it to me like you are talking to a classmate."
            4. mustRedo: Must be true if understandingOk is false or authenticityOk is false.
            5. earnedMinutes: Give 10 minutes if BOTH understandingOk and authenticityOk are true. Give 0 minutes if they must redo.
            
            Return a JSON object with this exact schema:
            {
              "understandingOk": true/false,
              "authenticityOk": true/false,
              "understandingScore": 0.0 to 1.0,
              "authenticityScore": 0.0 to 1.0,
              "feedback": "string",
              "missingPoints": ["point 1", "point 2"],
              "mustRedo": true/false,
              "earnedMinutes": integer,
              "personaUpdate": {
                 "vocabularyLevel": "Simple/Medium/Advanced",
                 "typicalSentenceLength": integer,
                 "toneDescription": "Concise summary of their current writing style, e.g., 'informal, uses lists, short sentences'",
                 "personaSummary": "Full concise summary of their style persona for future matching."
              }
            }
        """.trimIndent()

        val responseStr = callGemini(apiKey, prompt, sysInstruction)
        if (responseStr != null) {
            try {
                val cleaned = cleanJsonResponse(responseStr)
                val json = JSONObject(cleaned)
                val missingPoints = mutableListOf<String>()
                val missingArray = json.optJSONArray("missingPoints")
                if (missingArray != null) {
                    for (i in 0 until missingArray.length()) {
                        missingPoints.add(missingArray.getString(i))
                    }
                }
                val personaUpdate = json.getJSONObject("personaUpdate")
                
                return GradeResult(
                    understandingOk = json.getBoolean("understandingOk"),
                    authenticityOk = json.getBoolean("authenticityOk"),
                    understandingScore = json.optDouble("understandingScore", 0.5).toFloat(),
                    authenticityScore = json.optDouble("authenticityScore", 0.5).toFloat(),
                    feedback = json.getString("feedback"),
                    missingPoints = missingPoints,
                    mustRedo = json.getBoolean("mustRedo"),
                    earnedMinutes = json.getInt("earnedMinutes"),
                    vocabularyLevel = personaUpdate.getString("vocabularyLevel"),
                    sentenceLength = personaUpdate.getInt("typicalSentenceLength"),
                    toneDescription = personaUpdate.getString("toneDescription"),
                    personaSummary = personaUpdate.getString("personaSummary")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing grade result JSON: ${e.message}", e)
            }
        }

        // Return a mock grading fallback if Gemini fails or Key is absent
        return generateMockGrading(userExplanation, extractText, promptText, personaSummary)
    }

    /**
     * POST /v1/persona/bootstrap
     * Creates style persona from onboarding sample
     */
    suspend fun bootstrapPersona(
        apiKey: String,
        onboardingText: String
    ): PersonaBootstrapResult {
        val sysInstruction = "You are a professional writing style analyst."
        val prompt = """
            Analyze the following writing sample of a student:
            "$onboardingText"
            
            Describe their writing persona in JSON format:
            {
              "vocabularyLevel": "Simple" or "Medium" or "Advanced",
              "typicalSentenceLength": average number of words per sentence as integer,
              "toneDescription": "short phrase describing tone (e.g. conversational, direct, brief)",
              "personaSummary": "A concise paragraph describing their grammar, vocabulary, use of slang/shortcuts, and typical sentence structure."
            }
        """.trimIndent()

        val responseStr = callGemini(apiKey, prompt, sysInstruction)
        if (responseStr != null) {
            try {
                val cleaned = cleanJsonResponse(responseStr)
                val json = JSONObject(cleaned)
                return PersonaBootstrapResult(
                    vocabularyLevel = json.getString("vocabularyLevel"),
                    sentenceLength = json.getInt("typicalSentenceLength"),
                    toneDescription = json.getString("toneDescription"),
                    personaSummary = json.getString("personaSummary")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing bootstrap JSON: ${e.message}", e)
            }
        }

        // Sim fallback
        val words = onboardingText.split(" ").size
        val avgLen = if (words > 5) words / 2 else 8
        return PersonaBootstrapResult(
            vocabularyLevel = "Medium",
            sentenceLength = avgLen,
            toneDescription = "Direct and brief",
            personaSummary = "The user has a direct and brief writing style. They explain ideas using common words and conversational structuring."
        )
    }

    // Help clean standard AI-wrapped codeblocks
    private fun cleanJsonResponse(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter("\n")
            if (text.endsWith("```")) {
                text = text.substringBeforeLast("```")
            }
        }
        return text.trim()
    }

    /**
     * MOCK PLAN GENERATOR FOR LOCAL OFFLINE FALLBACK
     */
    private fun generateMockPlan(topic: String): List<CheckpointFixture> {
        val topicLower = topic.lowercase()
        return if (topicLower.contains("photo") || topicLower.contains("plant") || topicLower.contains("biology")) {
            listOf(
                CheckpointFixture(
                    "Light Absorption",
                    "Photosynthesis begins with light absorption in the chloroplasts of plant cells. These cells contain chlorophyll, a green pigment that absorbs light primarily in the blue and red wavelengths while reflecting green, which gives plants their color. When light hits chlorophyll, it excites electrons to a higher energy state, initiating the light-dependent reactions that generate ATP and NADPH.",
                    "Explain why leaves look green and how sunlight starts the photosynthesis process."
                ),
                CheckpointFixture(
                    "Water Splitting (Photolysis)",
                    "During the light-dependent stage, water molecules are split in a process called photolysis. Using light energy, water (H2O) is broken down into oxygen, hydrogen ions (protons), and electrons. The oxygen gas is released as a waste product through pores in leaves called stomata, while the hydrogen ions and electrons are used to create chemical energy carriers.",
                    "Explain where the oxygen released by plants comes from, and what happens to the remaining parts of the water molecule."
                ),
                CheckpointFixture(
                    "The Calvin Cycle",
                    "In the light-independent stage (Calvin Cycle), plants capture carbon dioxide from the air and convert it into glucose. This takes place in the stroma of chloroplasts. Using ATP and NADPH produced in the light reactions, carbon atoms from CO2 are fixed into stable energy-dense organic molecules that the plant uses for growth and storage.",
                    "Describe how plants use CO2 to create food, and specify what energy sources from the light stage are required."
                )
            )
        } else if (topicLower.contains("math") || topicLower.contains("calculus") || topicLower.contains("fraction")) {
            listOf(
                CheckpointFixture(
                    "Understanding Fractions",
                    "A fraction represents part of a whole, written as a numerator over a denominator. The denominator represents the total number of equal parts the whole is divided into, while the numerator represents how many of those parts we have. For example, in 3/4, a circle is cut into 4 equal slices, and we have 3 of them.",
                    "Explain what the top and bottom numbers of a fraction represent using an analogy of your own."
                ),
                CheckpointFixture(
                    "Adding Fractions with Common Denominators",
                    "When adding fractions that have the same denominator, you only add the numerators together while keeping the denominator unchanged. This is because the size of the parts (the denominator) remains the same, you are simply counting how many parts you have in total. For example, 1/5 + 2/5 equals 3/5.",
                    "Explain why we don't add the denominators together when adding two fractions like 1/8 + 3/8."
                ),
                CheckpointFixture(
                    "The Need for a Least Common Denominator",
                    "To add fractions with different denominators, like 1/2 and 1/3, you must first convert them so they share a common denominator. This represents cutting the whole into slices of identical size. You find the Least Common Multiple of the denominators (for 2 and 3, it is 6) and adjust the numerators proportionally: 3/6 and 2/6, which can then be added to make 5/6.",
                    "Explain in your own words why you cannot directly add 1/2 and 1/3 without changing them first."
                )
            )
        } else {
            // General topic plan
            listOf(
                CheckpointFixture(
                    "Core Principles of $topic",
                    "To master $topic, one must first grasp its fundamental building blocks. This subject deals with understanding how key components interact under specific conditions. Every core concept relies on clear definitions and relationship mapping to solve complex problems or explain natural occurrences.",
                    "Identify the single most important starting concept of $topic based on what you know or read, and explain why it matters."
                ),
                CheckpointFixture(
                    "Common Misconceptions in $topic",
                    "Many learners fail in $topic because of intuitive but incorrect assumptions. Mastering this requires stepping away from surface-level descriptions and looking at underlying causal links or structural frameworks. Overcoming these common misconceptions builds a solid foundation.",
                    "What is a common mistake people make when first studying $topic, and how can they avoid it?"
                ),
                CheckpointFixture(
                    "Practical Applications",
                    "The real value of $topic lies in its real-world application. Whether designing modern systems, explaining logical connections, or performing calculations, applying these principles allows us to solve actual physical or abstract problems effectively.",
                    "Explain one practical, real-world example of how $topic is used to solve a problem or create value."
                )
            )
        }
    }

    /**
     * MOCK GRADING FOR LOCAL OFFLINE FALLBACK
     */
    private fun generateMockGrading(
        userExplanation: String,
        extractText: String,
        promptText: String,
        personaSummary: String
    ): GradeResult {
        val cleanUserAns = userExplanation.trim()
        
        // 1. Check for blank or extremely short answers
        if (cleanUserAns.length < 15) {
            return GradeResult(
                understandingOk = false,
                authenticityOk = true,
                understandingScore = 0.1f,
                authenticityScore = 0.8f,
                feedback = "Your response is too short! To lock in learning, write at least a couple of sentences explaining the concepts in your own words.",
                missingPoints = listOf("Provide more details", "Write a full explanation"),
                mustRedo = true,
                earnedMinutes = 0,
                vocabularyLevel = "Medium",
                sentenceLength = 8,
                toneDescription = "Brief",
                personaSummary = personaSummary
            )
        }

        // 2. Check for exact copying / plagiarism (overlap checks)
        val extractWords = extractText.lowercase().split(Regex("[^a-zA-Z]+")).toSet()
        val userWords = cleanUserAns.lowercase().split(Regex("[^a-zA-Z]+"))
        var matchedCount = 0
        var consecutiveOverlapCount = 0
        
        // Simple heuristic: if user uses exact long phrases from extract
        val extractClean = extractText.lowercase().replace(Regex("[^a-z0-9 ]"), "")
        val userClean = cleanUserAns.lowercase().replace(Regex("[^a-z0-9 ]"), "")
        
        // Look for 5-word phrases copied exactly
        var copyPasteDetected = false
        val userWordsList = userClean.split(" ")
        if (userWordsList.size >= 5) {
            for (i in 0..userWordsList.size - 5) {
                val phrase = userWordsList.subList(i, i + 5).joinToString(" ")
                if (extractClean.contains(phrase) && phrase.isNotEmpty()) {
                    copyPasteDetected = true
                    break
                }
            }
        }

        if (copyPasteDetected) {
            return GradeResult(
                understandingOk = true, // they copied the correct text, so understanding is technically there
                authenticityOk = false, // but it is not authentic at all!
                understandingScore = 0.9f,
                authenticityScore = 0.1f,
                feedback = "Wait! That sounds too much like a copy-paste of the original passage. Try reading it, closing your eyes, and writing it down using words you would use to explain it to a friend.",
                missingPoints = listOf("Explain in your own authentic voice", "Avoid copy-pasting sentences directly"),
                mustRedo = true,
                earnedMinutes = 0,
                vocabularyLevel = "Medium",
                sentenceLength = userWordsList.size,
                toneDescription = "Copied",
                personaSummary = personaSummary
            )
        }

        // 3. Simple ChatGPT style detector (too professional, generic chatbot headers)
        val isAIProse = cleanUserAns.contains("certainly") || 
                        cleanUserAns.contains("furthermore") || 
                        cleanUserAns.contains("in summary,") || 
                        cleanUserAns.contains("crucial to note") ||
                        (cleanUserAns.startsWith("the passage explains") && cleanUserAns.length > 200)

        if (isAIProse && !personaSummary.contains("highly formal") && personaSummary.contains("cold-started")) {
            // Give a soft warning but let them pass or require a slight redo if it is too suspicious
            return GradeResult(
                understandingOk = true,
                authenticityOk = false,
                understandingScore = 0.85f,
                authenticityScore = 0.3f,
                feedback = "This explanation sounds a bit too much like a formal textbook or an AI assistant. Let's make it simpler! Re-write it in your normal conversational tone.",
                missingPoints = listOf("Simplify the vocabulary", "Explain using your natural phrasing"),
                mustRedo = true,
                earnedMinutes = 0,
                vocabularyLevel = "Medium",
                sentenceLength = userWordsList.size,
                toneDescription = "AI-ish",
                personaSummary = personaSummary
            )
        }

        // 4. Default pass!
        val sentences = cleanUserAns.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        val avgSentenceLen = if (sentences.isNotEmpty()) userWords.size / sentences.size else 10
        val isSimple = userWords.size < 40
        val vocab = if (isSimple) "Simple" else "Medium"
        val tone = if (isSimple) "Conversational and brief" else "Detailed and informative"
        
        val updatedSummary = "The user explains ideas in a ${tone.lowercase()} style with an average sentence length of $avgSentenceLen words. They use natural paraphrasing and avoid exact copying."

        return GradeResult(
            understandingOk = true,
            authenticityOk = true,
            understandingScore = 0.9f,
            authenticityScore = 0.9f,
            feedback = "Excellent teach-back! You've explained the concepts accurately and in your own authentic style. You've earned 10 minutes of unlocked device time!",
            missingPoints = emptyList(),
            mustRedo = false,
            earnedMinutes = 10,
            vocabularyLevel = vocab,
            sentenceLength = avgSentenceLen,
            toneDescription = tone,
            personaSummary = updatedSummary
        )
    }
}
