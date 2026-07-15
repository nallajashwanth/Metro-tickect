package com.example.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

class MetroViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = Room.databaseBuilder(
        application,
        MetroDatabase::class.java,
        "metro_hyd_db"
    ).fallbackToDestructiveMigration().build()

    private val repository = MetroRepository(db.metroDao())

    // Language State
    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Profile State
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Booked Tickets
    val bookedTickets: StateFlow<List<BookedTicket>> = repository.bookedTickets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Tab State
    private val _currentTab = MutableStateFlow("BOOK") // "BOOK", "SCHEDULES", "PASSES", "PROFILE", "AI"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Booking Form States
    private val _sourceStation = MutableStateFlow("Miyapur")
    val sourceStation: StateFlow<String> = _sourceStation.asStateFlow()

    private val _destStation = MutableStateFlow("Ameerpet")
    val destStation: StateFlow<String> = _destStation.asStateFlow()

    private val _ticketType = MutableStateFlow("SINGLE") // "SINGLE", "RETURN", "PASS"
    val ticketType: StateFlow<String> = _ticketType.asStateFlow()

    // Booking / Wallet Actions Status
    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    // Chatbot States
    private val _chatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val chatHistory: StateFlow<List<Pair<String, String>>> = _chatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Active Simulated Trip State
    private val _activeTripTicket = MutableStateFlow<BookedTicket?>(null)
    val activeTripTicket: StateFlow<BookedTicket?> = _activeTripTicket.asStateFlow()

    private val _tripStations = MutableStateFlow<List<MetroStation>>(emptyList())
    val tripStations: StateFlow<List<MetroStation>> = _tripStations.asStateFlow()

    private val _currentTripStationIndex = MutableStateFlow(0)
    val currentTripStationIndex: StateFlow<Int> = _currentTripStationIndex.asStateFlow()

    private val _tripDistanceLeft = MutableStateFlow(0.0)
    val tripDistanceLeft: StateFlow<Double> = _tripDistanceLeft.asStateFlow()

    private val _tripTimeLeft = MutableStateFlow(0)
    val tripTimeLeft: StateFlow<Int> = _tripTimeLeft.asStateFlow()

    private val _tripAnnouncement = MutableStateFlow("")
    val tripAnnouncement: StateFlow<String> = _tripAnnouncement.asStateFlow()

    // TextToSpeech for Voice Announcements
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Voice Input Stimulation Overlay
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    init {
        // Initialize Default User Profile
        viewModelScope.launch {
            val profile = repository.getOrCreateProfile()
            val savedLang = when (profile.preferredLanguage) {
                "TELUGU" -> AppLanguage.TELUGU
                "HINDI" -> AppLanguage.HINDI
                else -> AppLanguage.ENGLISH
            }
            _appLanguage.value = savedLang
        }
        // Init TTS
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("MetroVM", "TTS Init failed", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let {
                val result = it.setLanguage(Locale("en", "IN"))
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTtsReady = true
                }
            }
        }
    }

    fun speakAnnouncement(text: String) {
        if (isTtsReady) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MetroAnnouncement")
            } catch (e: Exception) {
                Log.e("MetroVM", "Error speaking announcement", e)
            }
        }
    }

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    fun setSourceStation(station: String) {
        _sourceStation.value = station
    }

    fun setDestStation(station: String) {
        _destStation.value = station
    }

    fun setTicketType(type: String) {
        _ticketType.value = type
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun changeLanguage(lang: AppLanguage) {
        _appLanguage.value = lang
        viewModelScope.launch {
            val current = userProfile.value
            if (current != null) {
                repository.updateProfile(current.name, current.phoneNumber, lang.name)
            }
        }
    }

    fun updateProfileInfo(name: String, phone: String) {
        viewModelScope.launch {
            repository.updateProfile(name, phone, appLanguage.value.name)
            _uiMessage.value = Translations.getString("profile_updated", appLanguage.value)
        }
    }

    fun addMoney(amount: Double) {
        viewModelScope.launch {
            val success = repository.addMoneyToWallet(amount)
            if (success) {
                _uiMessage.value = "+₹$amount added to wallet"
            }
        }
    }

    fun bookTicketDirectly() {
        viewModelScope.launch {
            val src = sourceStation.value
            val dst = destStation.value
            val type = ticketType.value

            val route = MetroNetwork.findRoute(src, dst) ?: return@launch
            val baseFare = route.fare
            val finalFare = when (type) {
                "RETURN" -> baseFare * 1.8
                "PASS" -> 120.00 // Fixed Daily Pass rate
                else -> baseFare
            }

            when (val res = repository.bookTicket(src, dst, type, finalFare)) {
                is BookedTicketResult.Success -> {
                    _uiMessage.value = if (type == "PASS") {
                        Translations.getString("pass_bought", appLanguage.value)
                    } else {
                        Translations.getString("ticket_booked", appLanguage.value)
                    }
                }
                is BookedTicketResult.Error -> {
                    if (res.message == "INSUFFICIENT_BALANCE") {
                        _uiMessage.value = Translations.getString("insufficient_balance", appLanguage.value)
                    } else {
                        _uiMessage.value = res.message
                    }
                }
            }
        }
    }

    // AI Chat Bot Q&A
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        val currentHistory = _chatHistory.value
        _chatHistory.value = currentHistory + (message to "")
        _isAiLoading.value = true

        viewModelScope.launch {
            val response = GeminiService.getAiResponse(message, currentHistory)
            
            // Check for potential Direct Booking command `[BOOK: Source to Destination]`
            var cleanResponse = response
            val bookRegex = Regex("\\[BOOK:\\s*(.+?)\\s*to\\s*(.+?)\\]")
            val match = bookRegex.find(response)
            
            if (match != null) {
                val extractedSource = match.groupValues[1].trim()
                val extractedDest = match.groupValues[2].trim()
                
                val validSource = MetroNetwork.getStationByName(extractedSource)
                val validDest = MetroNetwork.getStationByName(extractedDest)
                
                if (validSource != null && validDest != null) {
                    _sourceStation.value = validSource.name
                    _destStation.value = validDest.name
                    _ticketType.value = "SINGLE"
                    // Redirect to Book tab
                    _currentTab.value = "BOOK"
                    cleanResponse = response.replace(match.value, "").trim()
                }
            }

            _chatHistory.value = currentHistory + (message to cleanResponse)
            _isAiLoading.value = false
            
            // Speak response if in voice mode or simple assistant response
            if (appLanguage.value == AppLanguage.ENGLISH) {
                speakAnnouncement(cleanResponse.take(150)) // Speak short summary
            }
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }

    // Voice Input Simulation
    fun triggerVoiceListening() {
        _isListening.value = true
        // Simulate speech recognition results after 2.5 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            _isListening.value = false
            val simulatedSpeech = when ((1..5).random()) {
                1 -> "Book ticket from Miyapur to Ameerpet"
                2 -> "How much is the fare from HITEC City to Secunderabad East?"
                3 -> "I want to go to Charminar from Nagole"
                4 -> "Recommend the fastest route from Begumpet to L.B. Nagar"
                else -> "Book ticket from Parade Ground to Kukatpally"
            }
            sendChatMessage(simulatedSpeech)
        }, 2500)
    }

    // GPS / Ride Simulation Block
    private var simulationHandler: Handler? = null
    private var simulationRunnable: Runnable? = null

    fun startTripSimulation(ticket: BookedTicket) {
        stopTripSimulation() // Clean up any active trip

        val route = MetroNetwork.findRoute(ticket.sourceStation, ticket.destStation) ?: return
        val routeStations = route.stations
        if (routeStations.isEmpty()) return

        _activeTripTicket.value = ticket
        _tripStations.value = routeStations
        _currentTripStationIndex.value = 0
        _tripDistanceLeft.value = route.distanceKm
        _tripTimeLeft.value = route.durationMin
        _tripAnnouncement.value = ""

        // Mark ticket as active in UI, start ticker
        runTripStep()
    }

    private fun runTripStep() {
        val index = _currentTripStationIndex.value
        val stationsList = _tripStations.value
        if (index >= stationsList.size) {
            // Arrived!
            stopTripSimulation()
            return
        }

        val currentStation = stationsList[index]
        val isLast = index == stationsList.size - 1
        val isSecondToLast = index == stationsList.size - 2

        // Distance & travel calculations
        val stationsLeft = stationsList.size - 1 - index
        val distanceLeft = (stationsLeft * 1.3).coerceAtLeast(0.0)
        val timeLeft = (stationsLeft * 2).coerceAtLeast(0)

        _tripDistanceLeft.value = distanceLeft
        _tripTimeLeft.value = timeLeft

        // Formulate Announcements
        val announcement = buildString {
            if (index == 0) {
                val nextStation = if (stationsList.size > 1) stationsList[1] else currentStation
                // English representation
                val boardMsg = Translations.getString("board_towards", appLanguage.value)
                    .format(stationsList.last().name)
                val nextMsg = Translations.getString("next_station", appLanguage.value)
                    .format(nextStation.name)
                append("$boardMsg. $nextMsg.")
            } else if (isLast) {
                val arrivedMsg = Translations.getString("exit_left", appLanguage.value)
                append("$arrivedMsg.")
            } else {
                val nextStation = stationsList[index + 1]
                val nextMsg = Translations.getString("next_station", appLanguage.value)
                    .format(nextStation.name)
                append(nextMsg)
                if (currentStation.isInterchange) {
                    val changeMsg = Translations.getString("change_here", appLanguage.value)
                        .format(currentStation.line.displayNameEn)
                    append(". $changeMsg.")
                }
            }

            if (isSecondToLast) {
                append(" " + Translations.getString("arriving_one", appLanguage.value))
                append(". " + Translations.getString("prep_exit", appLanguage.value))
            }
        }

        _tripAnnouncement.value = announcement
        speakAnnouncement(announcement)

        // Schedule next step in 8 seconds
        simulationHandler = Handler(Looper.getMainLooper())
        simulationRunnable = Runnable {
            if (_activeTripTicket.value != null) {
                _currentTripStationIndex.value = index + 1
                if (index + 1 >= stationsList.size) {
                    // Update DB status to USED
                    viewModelScope.launch {
                        _activeTripTicket.value?.let {
                            repository.updateTicketStatus(it.id, "USED")
                        }
                        _activeTripTicket.value = null
                    }
                } else {
                    runTripStep()
                }
            }
        }
        simulationHandler?.postDelayed(simulationRunnable!!, 8000)
    }

    fun stopTripSimulation() {
        simulationHandler?.let {
            simulationRunnable?.let { run -> it.removeCallbacks(run) }
        }
        simulationHandler = null
        simulationRunnable = null
        _activeTripTicket.value = null
        _tripStations.value = emptyList()
        _tripAnnouncement.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        stopTripSimulation()
        tts?.let {
            it.stop()
            it.shutdown()
        }
    }
}
