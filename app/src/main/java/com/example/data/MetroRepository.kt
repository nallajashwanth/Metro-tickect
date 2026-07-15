package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class MetroRepository(private val dao: MetroDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()
    val bookedTickets: Flow<List<BookedTicket>> = dao.getBookedTicketsFlow()

    suspend fun getOrCreateProfile(): UserProfile {
        val existing = dao.getUserProfile()
        if (existing != null) return existing
        val defaultProfile = UserProfile(
            id = 1,
            name = "Srinivas Goud",
            phoneNumber = "+91 9876543210",
            walletBalance = 150.00,
            preferredLanguage = "ENGLISH"
        )
        dao.insertUserProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun updateProfile(name: String, phoneNumber: String, lang: String) {
        val current = dao.getUserProfile()
        val updated = UserProfile(
            id = 1,
            name = name,
            phoneNumber = phoneNumber,
            walletBalance = current?.walletBalance ?: 100.00,
            preferredLanguage = lang
        )
        dao.insertUserProfile(updated)
    }

    suspend fun addMoneyToWallet(amount: Double): Boolean {
        val current = dao.getUserProfile() ?: return false
        val updated = current.copy(walletBalance = current.walletBalance + amount)
        dao.insertUserProfile(updated)
        return true
    }

    suspend fun bookTicket(
        source: String,
        dest: String,
        ticketType: String, // "SINGLE", "RETURN", "PASS"
        fare: Double
    ): BookedTicketResult {
        val profile = dao.getUserProfile() ?: return BookedTicketResult.Error("Profile not found")
        if (profile.walletBalance < fare) {
            return BookedTicketResult.Error("INSUFFICIENT_BALANCE")
        }

        // Deduct fare
        val updatedProfile = profile.copy(walletBalance = profile.walletBalance - fare)
        dao.insertUserProfile(updatedProfile)

        // Generate QR code and metrics
        val qrData = "HYDMETRO-${ticketType}-${UUID.randomUUID().toString().take(8).uppercase()}"
        val route = MetroNetwork.findRoute(source, dest)
        val distance = route?.distanceKm ?: 5.0
        val duration = route?.durationMin ?: 15

        val ticket = BookedTicket(
            sourceStation = source,
            destStation = dest,
            fare = fare,
            ticketType = ticketType,
            timestamp = System.currentTimeMillis(),
            qrCodeData = qrData,
            status = "ACTIVE",
            distance = distance,
            duration = duration
        )

        val id = dao.insertBookedTicket(ticket)
        return BookedTicketResult.Success(ticket.copy(id = id.toInt()))
    }

    suspend fun updateTicketStatus(ticketId: Int, status: String) {
        val ticket = dao.getTicketById(ticketId) ?: return
        val updated = ticket.copy(status = status)
        dao.insertBookedTicket(updated)
    }
}

sealed class BookedTicketResult {
    data class Success(val ticket: BookedTicket) : BookedTicketResult()
    data class Error(val message: String) : BookedTicketResult()
}
