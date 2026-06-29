package in.hellocoolie.ui.porter

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import in.hellocoolie.R
import in.hellocoolie.data.model.*
import in.hellocoolie.data.repository.AuthRepository
import in.hellocoolie.data.repository.PorterRepository
import in.hellocoolie.databinding.*
import in.hellocoolie.services.SocketManager
import in.hellocoolie.ui.auth.AuthActivity
import in.hellocoolie.utils.Result
import in.hellocoolie.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── PorterViewModel ───────────────────────────────────────
@HiltViewModel
class PorterViewModel @Inject constructor(
    private val porterRepository: PorterRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _isOnline     = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    private val _earnings     = MutableStateFlow<EarningsAnalytics?>(null)
    val earnings: StateFlow<EarningsAnalytics?> = _earnings

    private val _wallet       = MutableStateFlow<WalletSummary?>(null)
    val wallet: StateFlow<WalletSummary?> = _wallet

    private val _activeBookings = MutableStateFlow<List<Any>>(emptyList())
    val activeBookings: StateFlow<List<Any>> = _activeBookings

    private val _uiState      = MutableStateFlow<PorterUiState>(PorterUiState.Idle)
    val uiState: StateFlow<PorterUiState> = _uiState

    val currentPorter get() = tokenManager.getPorter()

    fun toggleOnline(online: Boolean) = viewModelScope.launch {
        when (val r = porterRepository.toggleOnline(online)) {
            is Result.Success -> _isOnline.value = online
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun acceptBooking(bookingId: String) = viewModelScope.launch {
        _uiState.value = PorterUiState.Loading
        when (val r = porterRepository.acceptBooking(bookingId)) {
            is Result.Success -> _uiState.value = PorterUiState.BookingAccepted(bookingId)
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun rejectBooking(bookingId: String) = viewModelScope.launch {
        porterRepository.rejectBooking(bookingId)
        _uiState.value = PorterUiState.BookingRejected
    }

    fun verifyOtp(bookingId: String, otp: String) = viewModelScope.launch {
        _uiState.value = PorterUiState.Loading
        when (val r = porterRepository.verifyJobOtp(bookingId, otp)) {
            is Result.Success -> _uiState.value = PorterUiState.OtpVerified(bookingId)
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun completeJob(bookingId: String) = viewModelScope.launch {
        _uiState.value = PorterUiState.Loading
        when (val r = porterRepository.completeJob(bookingId)) {
            is Result.Success -> _uiState.value = PorterUiState.JobCompleted
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun cancelBooking(bookingId: String, reason: String) = viewModelScope.launch {
        when (val r = porterRepository.cancelBooking(bookingId, reason)) {
            is Result.Success -> _uiState.value = PorterUiState.BookingRejected
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun loadEarnings() = viewModelScope.launch {
        when (val r = porterRepository.getEarningsAnalytics()) {
            is Result.Success -> _earnings.value = r.data
            else -> {}
        }
    }

    fun loadWallet() = viewModelScope.launch {
        when (val r = porterRepository.getWallet()) {
            is Result.Success -> _wallet.value = r.data
            else -> {}
        }
    }

    fun withdraw(amount: Double, upiId: String?) = viewModelScope.launch {
        when (val r = porterRepository.withdraw(amount, upiId)) {
            is Result.Success -> { _uiState.value = PorterUiState.Success("₹${amount.toInt()} withdrawal initiated!"); loadWallet() }
            is Result.Error   -> _uiState.value = PorterUiState.Error(r.message)
            else -> {}
        }
    }

    fun sendSos(bookingId: String?, lat: Double?, lng: Double?) = viewModelScope.launch {
        porterRepository.porterSos(bookingId, lat, lng)
        _uiState.value = PorterUiState.Success("🆘 SOS raised. Help is on the way.")
    }

    fun logout() = viewModelScope.launch { authRepository.logout() }
}

sealed class PorterUiState {
    object Idle : PorterUiState()
    object Loading : PorterUiState()
    object BookingRejected : PorterUiState()
    object JobCompleted : PorterUiState()
    data class BookingAccepted(val bookingId: String) : PorterUiState()
    data class OtpVerified(val bookingId: String) : PorterUiState()
    data class Success(val message: String) : PorterUiState()
    data class Error(val message: String) : PorterUiState()
}

// ── PorterMainActivity ────────────────────────────────────
@AndroidEntryPoint
class PorterMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPorterMainBinding

    @Inject lateinit var socketManager: SocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPorterMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        socketManager.connect()
        setupNavigation()
        setupSocketListeners()
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showFragment(PorterDashboardFragment())
                R.id.nav_earnings  -> showFragment(PorterEarningsFragment())
                R.id.nav_wallet    -> showFragment(PorterWalletFragment())
                R.id.nav_profile   -> showFragment(PorterProfileFragment())
            }
            true
        }
        showFragment(PorterDashboardFragment())
    }

    private fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f)
            .commit()
    }

    // ── 🚨 UBER-LIKE BOOKING ALERT ─────────────────────────
    private fun setupSocketListeners() {
        socketManager.onNewBookingRequest = { request ->
            runOnUiThread { showBookingAlert(request) }
        }
        socketManager.onBookingConfirmed = { data ->
            runOnUiThread {
                val userName  = data.optString("traveller")?.let {
                    try { org.json.JSONObject(it).optString("name") } catch (e: Exception) { "Passenger" }
                } ?: "Passenger"
                showDialog("✅ Booking Confirmed!", "Passenger: $userName\nGo to the coach and ask for OTP.")
            }
        }
        socketManager.onJobStartedPorter = { data ->
            runOnUiThread {
                val userPhone = data.optString("userPhone", "")
                val userName  = data.optString("userName", "Passenger")
                AlertDialog.Builder(this)
                    .setTitle("🧳 Job Started!")
                    .setMessage("OTP verified. Passenger: $userName")
                    .setPositiveButton("Call Passenger") { _, _ ->
                        if (userPhone.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$userPhone")))
                        }
                    }
                    .setNegativeButton("OK", null)
                    .show()
            }
        }
    }

    private fun showBookingAlert(request: NewBookingRequest) {
        // Vibrate phone like emergency
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))

        // Play alert sound
        try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                // setDataSource(this@PorterMainActivity, Uri.parse("android.resource://${packageName}/${R.raw.booking_alert}"))
                prepare()
                start()
            }
        } catch (e: Exception) { /* ignore */ }

        // Show urgent dialog
        AlertDialog.Builder(this)
            .setTitle("🚨 नई बुकिंग! New Booking!")
            .setMessage("""
                📍 ${request.station}
                🚂 Train: ${request.trainNo ?: "N/A"}  Coach: ${request.coach ?: "N/A"}
                🧳 ${request.bags} bag(s) — ${request.bagWeight}
                📦 Drop: ${request.dropLocation}
                💰 Your earnings: ₹${request.fare.toInt()}
                ⏱️ Accept in ${request.expiresIn} seconds!
                ${if (request.traveller.isSenior) "👴 SENIOR CITIZEN — Priority booking" else ""}
                ${if (request.traveller.isWomanSolo) "👩 WOMAN ALONE — Priority booking" else ""}
            """.trimIndent())
            .setPositiveButton("✅ ACCEPT") { _, _ ->
                // Accept booking via ViewModel
                val frag = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (frag is PorterDashboardFragment) {
                    frag.acceptBooking(request.bookingId)
                }
            }
            .setNegativeButton("❌ Reject") { _, _ ->
                val frag = supportFragmentManager.findFragmentById(R.id.fragment_container)
                if (frag is PorterDashboardFragment) {
                    frag.rejectBooking(request.bookingId)
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    override fun onDestroy() { super.onDestroy(); socketManager.disconnect() }
}

// ── Porter Dashboard Fragment ─────────────────────────────
@AndroidEntryPoint
class PorterDashboardFragment : Fragment() {

    private var _binding: FragmentPorterDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PorterViewModel by viewModels()
    private var currentBookingId: String? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterDashboardBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val porter = viewModel.currentPorter
        binding.tvPorterName.text   = porter?.name ?: ""
        binding.tvPorterBadge.text  = "Badge: ${porter?.badgeNo}"
        binding.tvPorterStation.text= porter?.station ?: ""
        binding.tvRating.text       = "★ ${porter?.rating ?: 0.0}"

        // Online toggle
        binding.switchOnline.setOnCheckedChangeListener { _, checked ->
            viewModel.toggleOnline(checked)
            binding.tvOnlineStatus.text = if (checked) "🟢 Online — Ready for bookings" else "🔴 Offline"
        }

        // OTP input
        binding.btnVerifyOtp.setOnClickListener {
            val otp = binding.etOtp.text?.toString()?.trim() ?: ""
            if (otp.length != 6) { toast("Enter 6-digit OTP"); return@setOnClickListener }
            currentBookingId?.let { viewModel.verifyOtp(it, otp) }
                ?: toast("No active booking")
        }

        // Complete job
        binding.btnComplete.setOnClickListener {
            currentBookingId?.let {
                AlertDialog.Builder(requireContext())
                    .setTitle("Mark Job Complete?")
                    .setMessage("Confirm that you have delivered all luggage to the passenger.")
                    .setPositiveButton("Yes, Complete") { _, _ -> viewModel.completeJob(it) }
                    .setNegativeButton("Cancel", null)
                    .show()
            } ?: toast("No active booking")
        }

        // SOS
        binding.btnSos.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("🆘 Emergency SOS")
                .setMessage("This will alert your emergency contact and HelloCoolie admin immediately.")
                .setPositiveButton("SEND SOS") { _, _ -> viewModel.sendSos(currentBookingId, null, null) }
                .setNegativeButton("Cancel", null)
                .show()
        }

        observeState()
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is PorterUiState.BookingAccepted -> {
                            currentBookingId = state.bookingId
                            binding.cardOtp.visibility      = View.VISIBLE
                            binding.cardJobActive.visibility = View.GONE
                            toast("✅ Booking accepted! Go to coach and ask passenger for OTP.")
                        }
                        is PorterUiState.OtpVerified -> {
                            binding.cardOtp.visibility      = View.GONE
                            binding.cardJobActive.visibility = View.VISIBLE
                            toast("✅ OTP verified! Job started. Carry the luggage now.")
                        }
                        is PorterUiState.JobCompleted -> {
                            currentBookingId = null
                            binding.cardOtp.visibility      = View.GONE
                            binding.cardJobActive.visibility = View.GONE
                            toast("🎉 Job complete! Payment credited to your wallet.")
                        }
                        is PorterUiState.Error   -> toast(state.message)
                        is PorterUiState.Success -> toast(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    fun acceptBooking(bookingId: String) = viewModel.acceptBooking(bookingId)
    fun rejectBooking(bookingId: String) = viewModel.rejectBooking(bookingId)

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Porter Earnings Fragment ──────────────────────────────
@AndroidEntryPoint
class PorterEarningsFragment : Fragment() {

    private var _binding: FragmentPorterEarningsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PorterViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterEarningsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadEarnings()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.earnings.collect { analytics ->
                    analytics?.let { updateUI(it) }
                }
            }
        }
    }

    private fun updateUI(a: EarningsAnalytics) {
        binding.tvToday.text   = "₹${a.summary.today.earnings.toInt()}"
        binding.tvWeek.text    = "₹${a.summary.week.earnings.toInt()}"
        binding.tvMonth.text   = "₹${a.summary.month.earnings.toInt()}"
        binding.tvAllTime.text = "₹${a.summary.allTime.earnings.toInt()}"
        binding.tvTodayTrips.text = "${a.summary.today.bookings} trips"
        binding.tvWeekTrips.text  = "${a.summary.week.bookings} trips"
        a.bestDay?.let {
            binding.tvBestDay.text = "🏆 Best day: ${it.date} — ₹${it.earnings.toInt()} (${it.bookings} trips)"
            binding.cardBestDay.visibility = View.VISIBLE
        }
        // MPAndroidChart bar chart for last 7 days
        setupWeeklyChart(a.last7DaysChart)
    }

    private fun setupWeeklyChart(data: List<WeeklyEarning>) {
        if (data.isEmpty()) return
        val entries = data.mapIndexed { i, e ->
            com.github.mikephil.charting.data.BarEntry(i.toFloat(), e.earnings.toFloat())
        }
        val dataSet = com.github.mikephil.charting.data.BarDataSet(entries, "Earnings").apply {
            color = android.graphics.Color.parseColor("#F47920")
            valueTextColor = android.graphics.Color.parseColor("#374151")
            valueTextSize = 10f
        }
        binding.barChart.apply {
            this.data = com.github.mikephil.charting.data.BarData(dataSet)
            xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(data.map { it.dayName })
            xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled  = false
            legend.isEnabled     = false
            description.isEnabled = false
            animateY(800)
            invalidate()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Porter Wallet Fragment ────────────────────────────────
@AndroidEntryPoint
class PorterWalletFragment : Fragment() {

    private var _binding: FragmentPorterWalletBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PorterViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterWalletBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadWallet()

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.wallet.collect { w ->
                    w?.let {
                        binding.tvBalance.text = "₹${it.balance.toInt()}"
                        // Populate transaction list
                    }
                }
            }
        }

        binding.btnWithdraw.setOnClickListener { showWithdrawDialog() }
    }

    private fun showWithdrawDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Withdraw to UPI")
            .setMessage("Minimum withdrawal: ₹100\nYour UPI ID will be used from profile.")
            .setPositiveButton("Withdraw") { _, _ ->
                val amount = binding.etWithdrawAmount.text?.toString()?.toDoubleOrNull() ?: 0.0
                if (amount < 100) { toast("Minimum ₹100"); return@setPositiveButton }
                viewModel.withdraw(amount, null)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Porter Profile Fragment ───────────────────────────────
@AndroidEntryPoint
class PorterProfileFragment : Fragment() {

    private var _binding: FragmentPorterProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PorterViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterProfileBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val porter = viewModel.currentPorter
        binding.tvName.text    = porter?.name ?: ""
        binding.tvPhone.text   = "+91 ${porter?.phone}"
        binding.tvBadge.text   = "Badge: ${porter?.badgeNo}"
        binding.tvStation.text = porter?.station ?: ""
        binding.tvRating.text  = "★ ${porter?.rating ?: "—"} (${porter?.totalRatings ?: 0} ratings)"
        binding.tvBookings.text= "${porter?.totalBookings ?: 0} total trips"

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                viewModel.logout()
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
        }

        // Language switcher
        binding.rgLanguage.setOnCheckedChangeListener { _, id ->
            val lang = if (id == R.id.rb_hindi) "hi" else "en"
            toast("Language set to ${if (lang == "hi") "हिंदी" else "English"}")
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
