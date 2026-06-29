package com.hellocoolie.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import com.hellocoolie.data.model.*
import com.hellocoolie.data.repository.AuthRepository
import com.hellocoolie.databinding.*
import com.hellocoolie.ui.porter.PorterMainActivity
import com.hellocoolie.ui.user.UserMainActivity
import com.hellocoolie.utils.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.Lifecycle

// ── AuthViewModel ─────────────────────────────────────────
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun loginUser(phone: String, password: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        _state.value = when (val r = authRepository.loginUser(phone, password)) {
            is Result.Success -> AuthState.UserLoggedIn
            is Result.Error   -> AuthState.Error(r.message)
            else              -> AuthState.Error("Unknown error")
        }
    }

    fun loginPorter(phone: String, password: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        _state.value = when (val r = authRepository.loginPorter(phone, password)) {
            is Result.Success -> AuthState.PorterLoggedIn
            is Result.Error   -> AuthState.Error(r.message)
            else              -> AuthState.Error("Unknown error")
        }
    }

    fun registerUser(name: String, phone: String, password: String, dob: String, gender: String?) = viewModelScope.launch {
        _state.value = AuthState.Loading
        _state.value = when (val r = authRepository.registerUser(UserRegisterRequest(name, phone, password, dob, gender))) {
            is Result.Success -> AuthState.Registered("Registration successful! Please login.")
            is Result.Error   -> AuthState.Error(r.message)
            else              -> AuthState.Error("Unknown error")
        }
    }

    fun registerPorter(req: PorterRegisterRequest) = viewModelScope.launch {
        _state.value = AuthState.Loading
        _state.value = when (val r = authRepository.registerPorter(req)) {
            is Result.Success -> AuthState.Registered("Registration submitted! Admin will verify within 24–48 hours.")
            is Result.Error   -> AuthState.Error(r.message)
            else              -> AuthState.Error("Unknown error")
        }
    }

    fun resetState() { _state.value = AuthState.Idle }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object UserLoggedIn : AuthState()
    object PorterLoggedIn : AuthState()
    data class Registered(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

// ── AuthActivity ──────────────────────────────────────────
@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewPager.adapter = AuthPagerAdapter(this)
        binding.viewPager.isUserInputEnabled = false
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = when (pos) { 0 -> "Passenger"; else -> "Porter" }
        }.attach()
    }
}

class AuthPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount() = 2
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> UserLoginFragment()
        else -> PorterLoginFragment()
    }
}

// ── User Login ────────────────────────────────────────────
@AndroidEntryPoint
class UserLoginFragment : Fragment() {
    private var _binding: FragmentUserLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentUserLoginBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnLogin.setOnClickListener {
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            val pass  = binding.etPassword.text?.toString() ?: ""
            if (phone.length != 10) { toast("Enter valid 10-digit number"); return@setOnClickListener }
            if (pass.length < 6)    { toast("Password too short"); return@setOnClickListener }
            viewModel.loginUser(phone, pass)
        }
        binding.tvForgotPassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, ForgotPasswordFragment.newInstance("user"))
                .addToBackStack(null).commit()
        }
        binding.tvRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, UserRegisterFragment())
                .addToBackStack(null).commit()
        }
        collectState()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is AuthState.Loading      -> binding.btnLogin.isEnabled = false
                        is AuthState.UserLoggedIn -> {
                            startActivity(Intent(requireContext(), UserMainActivity::class.java))
                            requireActivity().finish()
                        }
                        is AuthState.Error -> { binding.btnLogin.isEnabled = true; toast(state.message) }
                        else               -> binding.btnLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Porter Login ──────────────────────────────────────────
@AndroidEntryPoint
class PorterLoginFragment : Fragment() {
    private var _binding: FragmentPorterLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterLoginBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPorterLogin.setOnClickListener {
            val phone = binding.etPorterPhone.text?.toString()?.trim() ?: ""
            val pass  = binding.etPorterPassword.text?.toString() ?: ""
            if (phone.length != 10) { toast("Enter valid 10-digit number"); return@setOnClickListener }
            if (pass.length < 6)    { toast("Password too short"); return@setOnClickListener }
            viewModel.loginPorter(phone, pass)
        }
        binding.tvPorterRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, PorterRegisterFragment())
                .addToBackStack(null).commit()
        }
        binding.tvPorterForgot.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, ForgotPasswordFragment.newInstance("porter"))
                .addToBackStack(null).commit()
        }
        collectState()
    }

    private fun collectState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is AuthState.Loading        -> binding.btnPorterLogin.isEnabled = false
                        is AuthState.PorterLoggedIn -> {
                            startActivity(Intent(requireContext(), PorterMainActivity::class.java))
                            requireActivity().finish()
                        }
                        is AuthState.Error -> { binding.btnPorterLogin.isEnabled = true; toast(state.message) }
                        else               -> binding.btnPorterLogin.isEnabled = true
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── User Register ─────────────────────────────────────────
@AndroidEntryPoint
class UserRegisterFragment : Fragment() {
    private var _binding: FragmentUserRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentUserRegisterBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnRegister.setOnClickListener {
            val name  = binding.etName.text?.toString()?.trim() ?: ""
            val phone = binding.etPhone.text?.toString()?.trim() ?: ""
            val pass  = binding.etPassword.text?.toString() ?: ""
            val dob   = binding.etDob.text?.toString()?.trim() ?: ""
            if (name.isEmpty() || phone.isEmpty() || pass.isEmpty() || dob.isEmpty()) {
                toast("Fill all required fields"); return@setOnClickListener
            }
            if (phone.length != 10) { toast("Invalid phone number"); return@setOnClickListener }
            if (pass.length < 6)    { toast("Password too short"); return@setOnClickListener }
            viewModel.registerUser(name, phone, pass, dob, null)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is AuthState.Registered -> { toast(state.message); parentFragmentManager.popBackStack() }
                        is AuthState.Error      -> toast(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Porter Register ───────────────────────────────────────
@AndroidEntryPoint
class PorterRegisterFragment : Fragment() {
    private var _binding: FragmentPorterRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentPorterRegisterBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnPorterSubmit.setOnClickListener {
            val req = PorterRegisterRequest(
                name             = binding.etPorterName.text?.toString()?.trim() ?: "",
                phone            = binding.etPorterPhone.text?.toString()?.trim() ?: "",
                password         = binding.etPorterPass.text?.toString() ?: "",
                aadhaarNo        = binding.etAadhaar.text?.toString()?.trim() ?: "",
                dateOfBirth      = binding.etDob.text?.toString()?.trim() ?: "",
                gender           = binding.spinnerGender.selectedItem?.toString(),
                address          = binding.etAddress.text?.toString()?.trim() ?: "",
                emergencyContact = binding.etEmergency.text?.toString()?.trim() ?: "",
                bloodGroup       = binding.etBloodGroup.text?.toString()?.trim(),
                badgeNo          = binding.etBadgeNo.text?.toString()?.trim() ?: "",
                station          = binding.etStation.text?.toString()?.trim() ?: "",
                shiftType        = if (binding.radioShift8.isChecked) "8hr" else "12hr",
                shiftStart       = binding.etShiftStart.text?.toString()?.trim(),
                shiftEnd         = binding.etShiftEnd.text?.toString()?.trim(),
                experienceYears  = binding.etExperience.text?.toString()?.toIntOrNull() ?: 0,
                upiId            = binding.etUpi.text?.toString()?.trim(),
                whatsappNo       = binding.etWhatsapp.text?.toString()?.trim()
            )
            if (req.name.isEmpty() || req.phone.isEmpty() || req.badgeNo.isEmpty() || req.station.isEmpty()) {
                toast("Fill all required fields"); return@setOnClickListener
            }
            viewModel.registerPorter(req)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is AuthState.Registered -> { toast(state.message); parentFragmentManager.popBackStack() }
                        is AuthState.Error      -> toast(state.message)
                        else -> {}
                    }
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── Forgot Password ───────────────────────────────────────
@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {
    companion object {
        fun newInstance(role: String) = ForgotPasswordFragment().apply {
            arguments = Bundle().apply { putString("role", role) }
        }
    }

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentForgotPasswordBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnReset.setOnClickListener {
            toast("Contact support to reset your password.")
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
