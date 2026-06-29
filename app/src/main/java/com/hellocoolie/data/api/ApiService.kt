package com.hellocoolie.data.api

import com.hellocoolie.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface HelloCoolieApi {

    // ── AUTH ──────────────────────────────────────────────
    @POST("auth/user/register")
    suspend fun registerUser(@Body request: UserRegisterRequest): Response<MessageResponse>

    @POST("auth/user/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<UserLoginResponse>

    @POST("auth/porter/register")
    suspend fun registerPorter(@Body request: PorterRegisterRequest): Response<MessageResponse>

    @POST("auth/porter/login")
    suspend fun loginPorter(@Body request: LoginRequest): Response<PorterLoginResponse>

    @POST("auth/send-otp")
    suspend fun sendOtp(@Body request: OtpRequest): Response<MessageResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<MessageResponse>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<MessageResponse>

    @PATCH("auth/fcm-token")
    suspend fun updateFcmToken(@Body body: Map<String, String>): Response<MessageResponse>

    // ── USER ──────────────────────────────────────────────
    @GET("user/profile")
    suspend fun getUserProfile(): Response<Map<String, User>>

    @PATCH("user/profile")
    suspend fun updateUserProfile(@Body body: Map<String, Any>): Response<MessageResponse>

    @POST("user/sos")
    suspend fun userSos(@Body request: SosRequest): Response<MessageResponse>

    @PATCH("user/language")
    suspend fun updateUserLanguage(@Body body: Map<String, String>): Response<MessageResponse>

    // ── BOOKINGS ──────────────────────────────────────────
    @GET("bookings/fare-preview")
    suspend fun getFarePreview(
        @Query("city_tier") cityTier: String,
        @Query("bag_count") bagCount: Int,
        @Query("bag_weight") bagWeight: String,
        @Query("drop_location") dropLocation: String
    ): Response<Map<String, FarePreview>>

    @GET("bookings/trolley-info")
    suspend fun getTrolleyInfo(@Query("station") station: String): Response<Map<String, Any>>

    @POST("bookings")
    suspend fun createBooking(@Body request: CreateBookingRequest): Response<CreateBookingResponse>

    @GET("bookings/my")
    suspend fun getMyBookings(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1
    ): Response<Map<String, Any>>

    @POST("bookings/{bookingId}/schedule")
    suspend fun scheduleBooking(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("bookings/{bookingId}/cancel")
    suspend fun cancelBookingUser(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("bookings/{bookingId}/rate")
    suspend fun rateBooking(
        @Path("bookingId") bookingId: String,
        @Body request: RatingRequest
    ): Response<MessageResponse>

    @POST("bookings/{bookingId}/dispute")
    suspend fun raiseDisputeUser(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("user/bookings/{bookingId}/trolley-response")
    suspend fun respondToTrolley(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, Boolean>
    ): Response<MessageResponse>

    // ── PORTER ────────────────────────────────────────────
    @GET("porter/profile")
    suspend fun getPorterProfile(): Response<Map<String, Porter>>

    @PATCH("porter/profile")
    suspend fun updatePorterProfile(@Body body: Map<String, Any>): Response<MessageResponse>

    @PATCH("porter/online")
    suspend fun toggleOnline(@Body body: Map<String, Boolean>): Response<Map<String, Any>>

    @GET("porter/wallet")
    suspend fun getWallet(): Response<WalletSummary>

    @POST("porter/wallet/withdraw")
    suspend fun withdraw(@Body request: WithdrawRequest): Response<MessageResponse>

    @GET("porter/earnings")
    suspend fun getEarningsSummary(): Response<Map<String, Any>>

    @GET("porter/earnings/analytics")
    suspend fun getEarningsAnalytics(): Response<EarningsAnalytics>

    @POST("porter/sos")
    suspend fun porterSos(@Body request: SosRequest): Response<MessageResponse>

    @PATCH("porter/language")
    suspend fun updatePorterLanguage(@Body body: Map<String, String>): Response<MessageResponse>

    @POST("porter/bookings/{bookingId}/accept")
    suspend fun acceptBooking(@Path("bookingId") bookingId: String): Response<Map<String, Any>>

    @POST("porter/bookings/{bookingId}/reject")
    suspend fun rejectBooking(@Path("bookingId") bookingId: String): Response<MessageResponse>

    @POST("porter/bookings/{bookingId}/verify-otp")
    suspend fun verifyJobOtp(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("porter/bookings/{bookingId}/complete")
    suspend fun completeJob(@Path("bookingId") bookingId: String): Response<MessageResponse>

    @POST("porter/bookings/{bookingId}/cancel")
    suspend fun cancelBookingPorter(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, String>
    ): Response<MessageResponse>

    @POST("porter/bookings/{bookingId}/offer-trolley")
    suspend fun offerTrolley(
        @Path("bookingId") bookingId: String,
        @Body body: Map<String, Any>
    ): Response<MessageResponse>

    @GET("porter/bookings")
    suspend fun getPorterBookings(@Query("status") status: String? = null): Response<Map<String, Any>>

    // ── PAYMENT ───────────────────────────────────────────
    @POST("payment/create-order")
    suspend fun createOrder(@Body body: Map<String, String>): Response<Map<String, Any>>

    @POST("payment/verify")
    suspend fun verifyPayment(@Body body: Map<String, String>): Response<MessageResponse>

    // ── I18N ──────────────────────────────────────────────
    @GET("i18n/strings")
    suspend fun getAppStrings(@Query("lang") lang: String): Response<Map<String, Any>>
}
