package in.hellocoolie.data.model

import com.google.gson.annotations.SerializedName

// ── AUTH ──────────────────────────────────────────────────
data class LoginRequest(
    val phone: String,
    val password: String
)

data class UserLoginResponse(
    val token: String,
    val user: User
)

data class PorterLoginResponse(
    val token: String,
    val porter: Porter
)

// ── USER ──────────────────────────────────────────────────
data class User(
    val id: String,
    val name: String,
    val phone: String,
    val gender: String?,
    @SerializedName("is_senior") val isSenior: Boolean = false,
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_banned") val isBanned: Boolean = false,
    @SerializedName("total_bookings") val totalBookings: Int = 0,
    @SerializedName("preferred_lang") val preferredLang: String = "en",
    @SerializedName("date_of_birth") val dateOfBirth: String?,
    @SerializedName("whatsapp_no") val whatsappNo: String?,
    @SerializedName("created_at") val createdAt: String?
)

// ── PORTER ────────────────────────────────────────────────
data class Porter(
    val id: String,
    val name: String,
    val phone: String,
    @SerializedName("badge_no") val badgeNo: String,
    val station: String,
    @SerializedName("city_tier") val cityTier: String = "y",
    @SerializedName("shift_type") val shiftType: String = "8hr",
    @SerializedName("shift_start") val shiftStart: String?,
    @SerializedName("shift_end") val shiftEnd: String?,
    val rating: Double = 0.0,
    @SerializedName("total_ratings") val totalRatings: Int = 0,
    @SerializedName("total_bookings") val totalBookings: Int = 0,
    @SerializedName("total_cancellations") val totalCancellations: Int = 0,
    @SerializedName("wallet_balance") val walletBalance: Double = 0.0,
    @SerializedName("is_online") val isOnline: Boolean = false,
    @SerializedName("is_on_job") val isOnJob: Boolean = false,
    val status: String = "pending",   // pending/approved/suspended
    @SerializedName("experience_years") val experienceYears: Int = 0,
    @SerializedName("upi_id") val upiId: String?,
    @SerializedName("preferred_lang") val preferredLang: String = "hi"
)

// ── BOOKING ───────────────────────────────────────────────
data class FarePreview(
    @SerializedName("baseFare") val baseFare: Double,
    @SerializedName("bagFare") val bagFare: Double,
    @SerializedName("distanceFare") val distanceFare: Double,
    @SerializedName("subtotal") val subtotal: Double,
    @SerializedName("platformFeePct") val platformFeePct: Double,
    @SerializedName("platformFee") val platformFee: Double,
    @SerializedName("porterAmount") val porterAmount: Double,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("seasonType") val seasonType: String,
    @SerializedName("cityTier") val cityTier: String,
    @SerializedName("twoPorterSuggested") val twoPorterSuggested: Boolean,
    val breakdown: FareBreakdown
)

data class FareBreakdown(
    val base: String,
    val bags: String,
    val distance: String,
    val platform: String,
    val total: String,
    val porter: String
)

data class CreateBookingRequest(
    @SerializedName("booking_for") val bookingFor: String = "myself",
    @SerializedName("traveller_name") val travellerName: String,
    @SerializedName("traveller_phone") val travellerPhone: String,
    @SerializedName("traveller_age") val travellerAge: Int?,
    @SerializedName("traveller_gender") val travellerGender: String?,
    @SerializedName("is_senior") val isSenior: Boolean = false,
    @SerializedName("is_woman_solo") val isWomanSolo: Boolean = false,
    @SerializedName("train_no") val trainNo: String?,
    @SerializedName("train_name") val trainName: String?,
    @SerializedName("from_station") val fromStation: String?,
    @SerializedName("to_station") val toStation: String?,
    @SerializedName("arrival_station") val arrivalStation: String,
    @SerializedName("arrival_time") val arrivalTime: String?,
    val coach: String?,
    @SerializedName("seat_no") val seatNo: String?,
    val pnr: String?,
    @SerializedName("bag_count") val bagCount: Int,
    @SerializedName("bag_weight") val bagWeight: String = "normal",
    @SerializedName("bag_details") val bagDetails: String?,
    @SerializedName("drop_location") val dropLocation: String = "platform",
    @SerializedName("two_porter_accepted") val twoPorterAccepted: Boolean = false,
    @SerializedName("payment_method") val paymentMethod: String = "online",
    @SerializedName("is_scheduled") val isScheduled: Boolean = false,
    @SerializedName("scheduled_for") val scheduledFor: String? = null
)

data class Booking(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("porter_id") val porterId: String?,
    @SerializedName("porter_name") val porterName: String?,
    @SerializedName("porter_phone") val porterPhone: String?,
    @SerializedName("traveller_name") val travellerName: String,
    @SerializedName("traveller_phone") val travellerPhone: String?,
    @SerializedName("arrival_station") val arrivalStation: String,
    @SerializedName("train_no") val trainNo: String?,
    @SerializedName("train_name") val trainName: String?,
    val coach: String?,
    @SerializedName("seat_no") val seatNo: String?,
    @SerializedName("bag_count") val bagCount: Int,
    @SerializedName("bag_weight") val bagWeight: String,
    @SerializedName("drop_location") val dropLocation: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("platform_fee") val platformFee: Double,
    @SerializedName("porter_amount") val porterAmount: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_status") val paymentStatus: String,
    val status: String,
    @SerializedName("otp_code") val otpCode: String?,
    @SerializedName("city_tier") val cityTier: String,
    @SerializedName("season_type") val seasonType: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("accepted_at") val acceptedAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    @SerializedName("arrival_time") val arrivalTime: String?,
    @SerializedName("is_senior") val isSenior: Boolean = false,
    @SerializedName("is_woman_solo") val isWomanSolo: Boolean = false,
    @SerializedName("two_porter_suggested") val twoPorterSuggested: Boolean = false,
    @SerializedName("needs_trolley") val needsTrolley: Boolean = false,
    @SerializedName("trolley_charge") val trolleyCharge: Double = 0.0
)

data class CreateBookingResponse(
    val bookingId: String,
    val fare: FarePreview,
    val status: String,
    val message: String,
    val twoPorterSuggested: Boolean
)

// ── PORTER NOTIFICATION (Socket.IO event) ─────────────────
data class NewBookingRequest(
    val bookingId: String,
    val expiresIn: Int = 30,
    val fare: Double,
    val totalFare: Double,
    val platformFee: Double,
    val bags: Int,
    val bagWeight: String,
    val dropLocation: String,
    val station: String,
    val trainNo: String?,
    val coach: String?,
    val seatNo: String?,
    val arrivalTime: String?,
    val isGroupBooking: Boolean = false,
    val traveller: TravellerInfo
)

data class TravellerInfo(
    val name: String,
    val isSenior: Boolean,
    val isWomanSolo: Boolean,
    val age: Int?,
    val gender: String?
)

// ── WALLET ────────────────────────────────────────────────
data class WalletSummary(
    val balance: Double,
    val transactions: List<WalletTransaction>
)

data class WalletTransaction(
    val id: String,
    @SerializedName("booking_id") val bookingId: String?,
    val type: String,
    val amount: Double,
    @SerializedName("balance_before") val balanceBefore: Double,
    @SerializedName("balance_after") val balanceAfter: Double,
    val description: String?,
    @SerializedName("created_at") val createdAt: String
)

data class WithdrawRequest(
    val amount: Double,
    @SerializedName("upi_id") val upiId: String?
)

// ── EARNINGS ──────────────────────────────────────────────
data class EarningsAnalytics(
    val summary: EarningsSummary,
    val bestDay: BestDay?,
    val bestMonth: BestMonth?,
    @SerializedName("last30Days") val last30Days: List<DailyEarning>,
    @SerializedName("last7DaysChart") val last7DaysChart: List<WeeklyEarning>
)

data class EarningsSummary(
    val today: EarningsEntry,
    val week: EarningsEntry,
    val month: EarningsEntry,
    val allTime: EarningsEntry
)

data class EarningsEntry(val earnings: Double, val bookings: Int)
data class BestDay(val date: String, val earnings: Double, val bookings: Int)
data class BestMonth(val month: String, @SerializedName("total_earnings") val totalEarnings: Double)
data class DailyEarning(val date: String, val earnings: Double, val bookings: Int)
data class WeeklyEarning(val day: String, @SerializedName("day_name") val dayName: String, val earnings: Double, val bookings: Int)

// ── RATING ────────────────────────────────────────────────
data class RatingRequest(
    @SerializedName("porter_rating") val porterRating: Int,
    @SerializedName("porter_review") val porterReview: String?,
    @SerializedName("porter_tags") val porterTags: List<String>?
)

// ── SOS ───────────────────────────────────────────────────
data class SosRequest(
    @SerializedName("booking_id") val bookingId: String?,
    val latitude: Double?,
    val longitude: Double?
)

// ── GENERIC ───────────────────────────────────────────────
data class MessageResponse(val message: String)
data class ApiError(val error: String)
data class OtpRequest(val identifier: String, val purpose: String, val role: String?)
data class VerifyOtpRequest(val identifier: String, val otp: String, val purpose: String)
data class ResetPasswordRequest(
    val identifier: String,
    @SerializedName("new_password") val newPassword: String,
    val role: String,
    @SerializedName("verification_field") val verificationField: String
)

// ── REGISTRATION ──────────────────────────────────────────
data class UserRegisterRequest(
    val name: String,
    val phone: String,
    val password: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    val gender: String?
)

data class PorterRegisterRequest(
    val name: String,
    val phone: String,
    val password: String,
    @SerializedName("aadhaar_no") val aadhaarNo: String,
    @SerializedName("date_of_birth") val dateOfBirth: String,
    val gender: String?,
    val address: String,
    @SerializedName("emergency_contact") val emergencyContact: String,
    @SerializedName("blood_group") val bloodGroup: String?,
    @SerializedName("badge_no") val badgeNo: String,
    val station: String,
    @SerializedName("shift_type") val shiftType: String,
    @SerializedName("shift_start") val shiftStart: String?,
    @SerializedName("shift_end") val shiftEnd: String?,
    @SerializedName("experience_years") val experienceYears: Int,
    @SerializedName("upi_id") val upiId: String?,
    @SerializedName("whatsapp_no") val whatsappNo: String?
)
