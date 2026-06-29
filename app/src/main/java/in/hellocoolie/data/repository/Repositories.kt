package in.hellocoolie.data.repository

import in.hellocoolie.data.api.HelloCoolieApi
import in.hellocoolie.data.model.*
import in.hellocoolie.utils.Result
import in.hellocoolie.utils.TokenManager
import in.hellocoolie.utils.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: HelloCoolieApi,
    private val tokenManager: TokenManager
) {
    suspend fun loginUser(phone: String, password: String): Result<UserLoginResponse> {
        val result = safeApiCall { api.loginUser(LoginRequest(phone, password)) }
        if (result is Result.Success) {
            tokenManager.saveUserSession(result.data.token, result.data.user)
        }
        return result
    }

    suspend fun loginPorter(phone: String, password: String): Result<PorterLoginResponse> {
        val result = safeApiCall { api.loginPorter(LoginRequest(phone, password)) }
        if (result is Result.Success) {
            tokenManager.savePorterSession(result.data.token, result.data.porter)
        }
        return result
    }

    suspend fun registerUser(request: UserRegisterRequest) =
        safeApiCall { api.registerUser(request) }

    suspend fun registerPorter(request: PorterRegisterRequest) =
        safeApiCall { api.registerPorter(request) }

    suspend fun sendOtp(identifier: String, purpose: String, role: String) =
        safeApiCall { api.sendOtp(OtpRequest(identifier, purpose, role)) }

    suspend fun verifyOtp(identifier: String, otp: String, purpose: String) =
        safeApiCall { api.verifyOtp(VerifyOtpRequest(identifier, otp, purpose)) }

    suspend fun resetPassword(identifier: String, newPassword: String, role: String, verificationField: String) =
        safeApiCall {
            api.resetPassword(ResetPasswordRequest(identifier, newPassword, role, verificationField))
        }

    suspend fun logout() = tokenManager.clearSession()

    fun isLoggedIn() = tokenManager.isLoggedIn()
    fun getRole()    = tokenManager.getRole()
    fun getUser()    = tokenManager.getUser()
    fun getPorter()  = tokenManager.getPorter()
}

// ── Booking Repository ────────────────────────────────────
@Singleton
class BookingRepository @Inject constructor(private val api: HelloCoolieApi) {

    suspend fun getFarePreview(cityTier: String, bagCount: Int, bagWeight: String, dropLocation: String) =
        safeApiCall { api.getFarePreview(cityTier, bagCount, bagWeight, dropLocation) }

    suspend fun createBooking(request: CreateBookingRequest) =
        safeApiCall { api.createBooking(request) }

    suspend fun getMyBookings(status: String? = null, page: Int = 1) =
        safeApiCall { api.getMyBookings(status, page) }

    suspend fun cancelBookingUser(bookingId: String, reason: String) =
        safeApiCall { api.cancelBookingUser(bookingId, mapOf("reason" to reason)) }

    suspend fun rateBooking(bookingId: String, rating: Int, review: String?, tags: List<String>?) =
        safeApiCall { api.rateBooking(bookingId, RatingRequest(rating, review, tags)) }

    suspend fun raiseDispute(bookingId: String, description: String) =
        safeApiCall { api.raiseDisputeUser(bookingId, mapOf("description" to description)) }

    suspend fun respondToTrolley(bookingId: String, accepted: Boolean) =
        safeApiCall { api.respondToTrolley(bookingId, mapOf("accepted" to accepted)) }
}

// ── Porter Repository ─────────────────────────────────────
@Singleton
class PorterRepository @Inject constructor(private val api: HelloCoolieApi) {

    suspend fun getProfile()          = safeApiCall { api.getPorterProfile() }
    suspend fun toggleOnline(online: Boolean) = safeApiCall { api.toggleOnline(mapOf("is_online" to online)) }
    suspend fun getWallet()           = safeApiCall { api.getWallet() }
    suspend fun withdraw(amount: Double, upiId: String?) = safeApiCall { api.withdraw(WithdrawRequest(amount, upiId)) }
    suspend fun getEarningsAnalytics()= safeApiCall { api.getEarningsAnalytics() }
    suspend fun getPorterBookings(status: String? = null) = safeApiCall { api.getPorterBookings(status) }

    suspend fun acceptBooking(bookingId: String) = safeApiCall { api.acceptBooking(bookingId) }
    suspend fun rejectBooking(bookingId: String) = safeApiCall { api.rejectBooking(bookingId) }
    suspend fun verifyJobOtp(bookingId: String, otp: String) =
        safeApiCall { api.verifyJobOtp(bookingId, mapOf("otp" to otp)) }
    suspend fun completeJob(bookingId: String)   = safeApiCall { api.completeJob(bookingId) }
    suspend fun cancelBooking(bookingId: String, reason: String) =
        safeApiCall { api.cancelBookingPorter(bookingId, mapOf("reason" to reason)) }
    suspend fun offerTrolley(bookingId: String, charge: Double) =
        safeApiCall { api.offerTrolley(bookingId, mapOf("trolley_charge" to charge)) }
    suspend fun porterSos(bookingId: String?, lat: Double?, lng: Double?) =
        safeApiCall { api.porterSos(SosRequest(bookingId, lat, lng)) }
}
