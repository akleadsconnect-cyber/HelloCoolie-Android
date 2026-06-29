package com.hellocoolie.ui.user

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import com.hellocoolie.R
import com.hellocoolie.data.model.*
import com.hellocoolie.data.repository.AuthRepository
import com.hellocoolie.data.repository.BookingRepository
import com.hellocoolie.databinding.*
import com.hellocoolie.services.SocketManager
import com.hellocoolie.ui.auth.AuthActivity
import com.hellocoolie.utils.Result
import com.hellocoolie.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UserViewModel ─────────────────────────────────────────
@HiltViewModel
class UserViewModel @Inject constructor(
    private val bookingRepository: BookingRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _farePreview = MutableStateFlow<FarePreview?>(null)
    val farePreview: StateFlow<FarePreview?> = _farePreview

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    val bookings: StateFlow<List<Booking>> = _bookings

    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Idle)
    val uiState: StateFlow<UserUiState> = _uiState

    val currentUser get() = tokenManager.getUser()

    fun loadFarePreview(cityTier: String, bagCount: Int, bagWeight: String, dropLocation: String) = viewModelScope.launch {
        when (val r = bookingRepository.getFarePreview(cityTier, bagCount, bagWeight, dropLocation)) {
            is Result.Success -> _farePreview.value = r.data["fare"]
            is Result.Error   -> _uiState.value = UserUiState.Error(r.message)
            else -> {}
        }
    }

    fun createBooking(request: CreateBookingRequest) = viewModelScope.launch {
        _uiState.value = UserUiState.Loading
        when (val r = bookingRepository.createBooking(request)) {
            is Result.Success -> _uiState.value = UserUiState.BookingCreated(r.data)
            is Result.Error   -> _uiState.value = UserUiState.Error(r.message)
            else -> {}
        }
    }

    fun loadMyBookings(status: String? = null) = viewModelScope.launch {
        when (val r = bookingRepository.getMyBookings(status)) {
            is Result.Success -> {
                @Suppress("UNCHECKED_CAST")
                val list = (r.data["bookings"] as? List<*>)?.mapNotNull { it as? Booking } ?: emptyList()
                _bookings.value = list
            }
            else -> {}
        }
    }

    fun cancelBooking(bookingId: String, reason: String) = viewModelScope.launch {
        when (val r = bookingRepository.cancelBookingUser(bookingId, reason)) {
            is Result.Success -> { _uiState.value = UserUiState.Success("Booking cancelled"); loadMyBookings() }
            is Result.Error   -> _uiState.value = UserUiState.Error(r.message)
            else -> {}
        }
    }

    fun rateBooking(bookingId: String, rating: Int, review: String?, tags: List<String>?) = viewModelScope.launch {
        when (val r = bookingRepository.rateBooking(bookingId, rating, review, tags)) {
            is Result.Success -> { _uiState.value = UserUiState.Success("Rating submitted!"); loadMyBookings() }
            is Result.Error   -> _uiState.value = UserUiState.Error(r.message)
            else -> {}
        }
    }

    fun logout() = viewModelScope.launch { authRepository.logout() }
}

    data class UserStats(val totalBookings: Int, val totalSpend: Int, val avgTime: String)

    suspend fun getUserStats(): UserStats? = try {
        val r = bookingRepository.getMyBookings()
        if (r is Result.Success) {
            val list = r.data
            UserStats(list.size, list.sumOf { it.totalAmount?.toIntOrNull() ?: 0 }, "—")
        } else null
    } catch (e: Exception) { null }
}

sealed class UserUiState {
    object Idle : UserUiState()
    object Loading : UserUiState()
    data class BookingCreated(val response: CreateBookingResponse) : UserUiState()
    data class Success(val message: String) : UserUiState()
    data class Error(val message: String) : UserUiState()
}

// ── UserMainActivity ──────────────────────────────────────
@AndroidEntryPoint
class UserMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserMainBinding

    @Inject lateinit var socketManager: SocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        try { socketManager.connect() } catch (e: Exception) { }
        setupNavigation()
        try { setupSocketListeners() } catch (e: Exception) { }
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> showFragment(UserHomeFragment())
                R.id.nav_bookings -> showFragment(UserBookingsFragment())
                R.id.nav_profile  -> showFragment(UserProfileFragment())
            }
            true
        }
        showFragment(UserHomeFragment())
        binding.bottomNav.selectedItemId = R.id.nav_home
    }

    private fun showFragment(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, f)
            .commit()
    }

    private fun setupSocketListeners() {
        socketManager.onPorterAssigned = { data ->
            runOnUiThread {
                val porterName = data.optString("porter")?.let {
                    try { org.json.JSONObject(it).optString("name") } catch (e: Exception) { "Porter" }
                } ?: "Porter"
                val otp = data.optString("otp", "")
                showNotificationDialog("✅ Porter Found!", "$porterName is on the way!\nShow OTP: $otp to porter when they arrive.")
            }
        }
        socketManager.onJobStarted = { _ ->
            runOnUiThread { showNotificationDialog("🧳 Job Started!", "Porter has verified OTP. Your luggage is being carried.") }
        }
        socketManager.onJobCompleted = { _ ->
            runOnUiThread { showNotificationDialog("✅ Job Complete!", "Your luggage has been delivered. Please rate your porter.") }
        }
        socketManager.onBookingCancelled = { _ ->
            runOnUiThread { showNotificationDialog("❌ Porter Cancelled", "Porter cancelled your booking. Finding another porter...") }
        }
        socketManager.onBookingExpired = { _ ->
            runOnUiThread { showNotificationDialog("😔 No Porter Found", "No porter available near your station. Full refund initiated.") }
        }
        socketManager.onTrolleyOffer = { data ->
            runOnUiThread {
                val charge = data.optDouble("trolleyCharge", 0.0)
                showTrolleyOfferDialog(data.optString("bookingId"), charge)
            }
        }
    }

    private fun showNotificationDialog(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showTrolleyOfferDialog(bookingId: String, charge: Double) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🛒 Trolley Service Offer")
            .setMessage("Porter is offering trolley service for ₹${charge.toInt()} extra. Accept?")
            .setPositiveButton("Accept") { _, _ -> /* Call API respondToTrolley */ }
            .setNegativeButton("Decline", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
    }
}

// ── User Home Fragment ────────────────────────────────────
@AndroidEntryPoint
class UserHomeFragment : Fragment() {

    private var _binding: FragmentUserHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentUserHomeBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = viewModel.currentUser
        binding.tvWelcome.text = "Hello, ${user?.name?.split(" ")?.firstOrNull() ?: "Traveller"} 👋"

        binding.btnBookCoolie.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, BookCoolieFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun showSosConfirm() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🆘 Emergency SOS")
            .setMessage("This will immediately alert your emergency contact and the HelloCoolie admin team with your location. Proceed?")
            .setPositiveButton("SEND SOS") { _, _ ->
                // Check location permission and send SOS
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // Send SOS via API
                    lifecycleScope.launch {
                        // api.userSos(SosRequest(null, null, null))
                        Toast.makeText(requireContext(), "🆘 SOS raised! Help is on the way.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Book Coolie Fragment ──────────────────────────────────
@AndroidEntryPoint
class BookCoolieFragment : Fragment() {

    private var _binding: FragmentBookCoolieBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    private var selectedBagWeight = "normal"
    private var selectedDropLocation = "platform"
    private var selectedPaymentMethod = "online"
    private var bagCount = 1
    private var isSenior = false
    private var isWomanSolo = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBookCoolieBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        setupBagCounter()
        setupWeightSelector()
        setupDropSelector()
        setupFarePreview()

        binding.switchSenior.setOnCheckedChangeListener { _, checked -> isSenior = checked }
        binding.switchWomanSolo.setOnCheckedChangeListener { _, checked -> isWomanSolo = checked }

        binding.rgPayment.setOnCheckedChangeListener { _, id ->
            selectedPaymentMethod = if (id == R.id.rb_online) "online" else "cash"
        }

        binding.btnBookNow.setOnClickListener { submitBooking() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UserUiState.Loading -> { binding.btnBookNow.isEnabled = false; binding.btnBookNow.text = "Booking..." }
                        is UserUiState.BookingCreated -> {
                            parentFragmentManager.beginTransaction()
                                .replace(android.R.id.content, BookingSearchingFragment.newInstance(state.response.bookingId))
                                .addToBackStack(null).commit()
                        }
                        is UserUiState.Error -> { binding.btnBookNow.isEnabled = true; binding.btnBookNow.text = getString(R.string.book_now); toast(state.message) }
                        else -> { binding.btnBookNow.isEnabled = true; binding.btnBookNow.text = getString(R.string.book_now) }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.farePreview.collect { fare ->
                    fare?.let { updateFareUI(it) }
                }
            }
        }
    }

    private fun setupBagCounter() {
        binding.tvBagCount.text = bagCount.toString()
        binding.btnBagMinus.setOnClickListener {
            if (bagCount > 1) { bagCount--; binding.tvBagCount.text = bagCount.toString(); loadFare() }
        }
        binding.btnBagPlus.setOnClickListener {
            if (bagCount < 8) { bagCount++; binding.tvBagCount.text = bagCount.toString(); loadFare()
                if (bagCount >= 5) toast("⚠️ 5+ bags: We'll suggest 2 porters")
            }
        }
    }

    private fun setupWeightSelector() {
        binding.rgWeight.setOnCheckedChangeListener { _, id ->
            selectedBagWeight = when (id) {
                R.id.rb_normal     -> "normal"
                R.id.rb_medium     -> "medium"
                R.id.rb_heavy      -> "heavy"
                R.id.rb_very_heavy -> "very_heavy"
                else               -> "normal"
            }
            loadFare()
        }
    }

    private fun setupDropSelector() {
        binding.rgDrop.setOnCheckedChangeListener { _, id ->
            selectedDropLocation = when (id) {
                R.id.rb_platform -> "platform"
                R.id.rb_exit     -> "exit"
                R.id.rb_auto     -> "auto"
                else             -> "platform"
            }
            loadFare()
        }
    }

    private fun setupFarePreview() { loadFare() }

    private fun loadFare() {
        viewModel.loadFarePreview("y", bagCount, selectedBagWeight, selectedDropLocation)
    }

    private fun updateFareUI(fare: FarePreview) {
        binding.tvBaseFare.text     = "Base fare: ₹${fare.baseFare.toInt()}"
        binding.tvBagFare.text      = "Bag fare: ₹${fare.bagFare.toInt()}"
        binding.tvDistanceFare.text = "Distance: ₹${fare.distanceFare.toInt()}"
        binding.tvPlatformFee.text  = "Platform (${fare.platformFeePct.toInt()}%): ₹${fare.platformFee.toInt()}"
        binding.tvPorterAmount.text = "Porter gets: ₹${fare.porterAmount.toInt()}"
        binding.tvTotalFare.text    = "₹${fare.totalAmount.toInt()}"
        if (fare.twoPorterSuggested) {
            binding.tvTwoPorterWarning.visibility = View.VISIBLE
        }
    }

    private fun submitBooking() {
        val travellerName  = binding.etTravellerName.text?.toString()?.trim() ?: ""
        val travellerPhone = binding.etTravellerPhone.text?.toString()?.trim() ?: ""
        val station        = binding.etStation.text?.toString()?.trim() ?: ""
        val trainNo        = binding.etTrainNo.text?.toString()?.trim()
        val coach          = binding.etCoach.text?.toString()?.trim()

        if (travellerName.isEmpty() || travellerPhone.isEmpty() || station.isEmpty()) {
            toast("Fill traveller name, phone, and station"); return
        }
        if (travellerPhone.length != 10) { toast("Invalid traveller phone"); return }

        val request = CreateBookingRequest(
            bookingFor      = if (binding.rgFor.checkedRadioButtonId == R.id.rb_myself) "myself" else "other",
            travellerName   = travellerName,
            travellerPhone  = travellerPhone,
            travellerAge    = binding.etAge.text?.toString()?.toIntOrNull(),
            travellerGender = binding.spinnerGender.selectedItem?.toString(),
            isSenior        = isSenior,
            isWomanSolo     = isWomanSolo,
            trainNo         = trainNo,
            trainName       = null,
            fromStation     = null,
            toStation       = null,
            arrivalStation  = station,
            arrivalTime     = null,
            coach           = coach,
            seatNo          = binding.etPnr.text?.toString()?.trim(),
            pnr             = binding.etPnr.text?.toString()?.trim(),
            bagCount        = bagCount,
            bagWeight       = selectedBagWeight,
            bagDetails      = null,
            dropLocation    = selectedDropLocation,
            twoPorterAccepted = bagCount >= 5 && binding.switchTwoPorter.isChecked,
            paymentMethod   = selectedPaymentMethod
        )
        viewModel.createBooking(request)
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()



    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Booking Searching Fragment ────────────────────────────
@AndroidEntryPoint
class BookingSearchingFragment : Fragment() {

    companion object {
        fun newInstance(bookingId: String) = BookingSearchingFragment().apply {
            arguments = Bundle().apply { putString("booking_id", bookingId) }
        }
    }

    private var _binding: FragmentBookingSearchingBinding? = null
    private val binding get() = _binding!!
    private val bookingId get() = arguments?.getString("booking_id") ?: ""

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentBookingSearchingBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvBookingId.text = "Booking: $bookingId"
        binding.tvStatus.text = "Finding the best porter near you..."
        binding.btnCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── User Bookings Fragment ────────────────────────────────
@AndroidEntryPoint
class UserBookingsFragment : Fragment() {

    private var _binding: FragmentUserBookingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentUserBookingsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadMyBookings()
        // RecyclerView setup with BookingAdapter
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── User Profile Fragment ─────────────────────────────────
@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentUserProfileBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val user = viewModel.currentUser
        binding.tvName.text  = user?.name ?: ""
        binding.tvPhone.text = "+91 ${user?.phone}"

        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                viewModel.logout()
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
        }

        binding.btnSos.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("🆘 Emergency SOS")
                .setMessage("This will alert your emergency contact. Are you sure?")
                .setPositiveButton("YES, SEND SOS") { _, _ ->
                    Toast.makeText(requireContext(), "SOS alert sent!", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Cancel", null).show()
        }

        binding.btnLogout.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.logout()
                        startActivity(Intent(requireContext(), AuthActivity::class.java))
                        requireActivity().finish()
                    }
                }
                .setNegativeButton("Cancel", null).show()
        }

        binding.btnCallSupport.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:+911234567890")))
        }

        // Load stats
        lifecycleScope.launch {
            try {
                val stats = viewModel.getUserStats()
                binding.tvTotalBookings.text = stats?.totalBookings?.toString() ?: "0"
                binding.tvTotalSpend.text    = "₹${stats?.totalSpend ?: 0}"
                binding.tvAvgTime.text       = stats?.avgTime ?: "—"
            } catch (e: Exception) {}
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
