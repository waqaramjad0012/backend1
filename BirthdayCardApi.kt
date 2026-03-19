// ─────────────────────────────────────────────────────────────────────────────
// FILE: BirthdayCardApi.kt
// Add to your Android project. Uses Retrofit + OkHttp (already common deps).
// ─────────────────────────────────────────────────────────────────────────────

// ── 1. Data models ───────────────────────────────────────────────────────────

data class CreateCardRequest(
    val recipientName: String,
    val age: Int,
    val message: String,
    val senderName: String = "",
    val theme: String = "pink"  // "pink" | "blue" | "gold" | "green"
)

data class CreateCardResponse(
    val success: Boolean,
    val slug: String,
    val url: String
)

// ── 2. Retrofit interface ─────────────────────────────────────────────────────

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface BirthdayApiService {
    @POST("api/cards")
    suspend fun createCard(@Body request: CreateCardRequest): Response<CreateCardResponse>
}

// ── 3. Retrofit instance (singleton) ─────────────────────────────────────────

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object BirthdayApiClient {
    private const val BASE_URL = "https://your-backend.vercel.app/" // 🔁 Replace

    val service: BirthdayApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BirthdayApiService::class.java)
    }
}

// ── 4. Repository ─────────────────────────────────────────────────────────────

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BirthdayCardRepository {

    suspend fun createCard(
        recipientName: String,
        age: Int,
        message: String,
        senderName: String = "",
        theme: String = "pink"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = BirthdayApiClient.service.createCard(
                CreateCardRequest(recipientName, age, message, senderName, theme)
            )
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()!!.url)
            } else {
                Result.failure(Exception("Failed to create card"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

// ── 5. ViewModel ──────────────────────────────────────────────────────────────

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CardCreationState {
    object Idle    : CardCreationState()
    object Loading : CardCreationState()
    data class Success(val url: String) : CardCreationState()
    data class Error(val message: String) : CardCreationState()
}

class BirthdayCardViewModel : ViewModel() {

    private val repository = BirthdayCardRepository()

    private val _state = MutableStateFlow<CardCreationState>(CardCreationState.Idle)
    val state: StateFlow<CardCreationState> = _state

    fun createCard(
        recipientName: String,
        age: Int,
        message: String,
        senderName: String,
        theme: String
    ) {
        viewModelScope.launch {
            _state.value = CardCreationState.Loading

            repository.createCard(recipientName, age, message, senderName, theme)
                .onSuccess { url ->
                    _state.value = CardCreationState.Success(url)
                }
                .onFailure { e ->
                    _state.value = CardCreationState.Error(e.message ?: "Unknown error")
                }
        }
    }
}

// ── 6. Fragment / Activity usage ──────────────────────────────────────────────

/*
In your Fragment (using ViewBinding + Kotlin coroutines):

class CreateBirthdayCardFragment : Fragment() {

    private val viewModel: BirthdayCardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreate.setOnClickListener {
            val name    = binding.etName.text.toString().trim()
            val age     = binding.etAge.text.toString().toIntOrNull() ?: 0
            val message = binding.etMessage.text.toString().trim()
            val sender  = binding.etSender.text.toString().trim()
            val theme   = binding.spinnerTheme.selectedItem.toString()

            if (name.isEmpty() || age == 0 || message.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.createCard(name, age, message, sender, theme)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is CardCreationState.Loading -> {
                        binding.progressBar.isVisible = true
                        binding.btnCreate.isEnabled   = false
                    }
                    is CardCreationState.Success -> {
                        binding.progressBar.isVisible = false
                        binding.btnCreate.isEnabled   = true
                        showShareSheet(state.url)          // 👇 see below
                    }
                    is CardCreationState.Error -> {
                        binding.progressBar.isVisible = false
                        binding.btnCreate.isEnabled   = true
                        Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    }
                    else -> {}
                }
            }
        }
    }

    // Opens Android native share sheet with the card URL
    private fun showShareSheet(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "🎂 I made you a birthday card! Open it here: $url")
        }
        startActivity(Intent.createChooser(intent, "Share Birthday Card via"))
    }
}
*/
