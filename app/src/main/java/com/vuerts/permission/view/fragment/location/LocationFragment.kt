package com.vuerts.permission.view.fragment.location

import android.Manifest
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.vuerts.permission.R
import com.vuerts.permission.databinding.FragmentLocationBinding
import com.vuerts.permission.domain.location.exception.LocationIsOffException
import com.vuerts.permission.presentation.location.model.LocationState
import com.vuerts.permission.presentation.location.viewmodel.LocationViewModel
import com.vuerts.permission.util.extensions.lifecycle.launchOnLifecycleDestroy
import com.vuerts.permission.util.extensions.lifecycle.repeatOnStarted
import com.vuerts.permission.util.permissionchecker.PermissionChecker
import org.koin.androidx.viewmodel.ext.android.viewModel

class LocationFragment : Fragment() {

    private val viewModel by viewModel<LocationViewModel>()

    private var binding: FragmentLocationBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentLocationBinding
        .inflate(inflater, container, false)
        .also(::binding::set)
        .also {
            viewLifecycleOwner.launchOnLifecycleDestroy { binding = null }
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = requireNotNull(binding)

        initButtons(binding)
        listenViewModel(binding, viewModel)
    }

    private fun initButtons(binding: FragmentLocationBinding) {
        binding.btnGetLocation.setOnClickListener {
            viewModel.onGetLocationButtonClicked()
        }
    }

    private fun listenViewModel(binding: FragmentLocationBinding, viewModel: LocationViewModel) {
        viewLifecycleOwner.repeatOnStarted {
            viewModel.locationStateFlow.collect {
                binding.tvLocation.text = when (it) {
                    LocationState.Empty -> {
                        getString(R.string.location_unknown)
                    }

                    is LocationState.Loaded -> {
                        getString(
                            R.string.location_patter,
                            it.location.latitude,
                            it.location.longitude,
                        )
                    }
                }
            }
        }

        viewLifecycleOwner.repeatOnStarted {
            viewModel.isLoading.collect {
                TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
                binding.progressLayout.isVisible = it
            }
        }

        viewLifecycleOwner.repeatOnStarted {
            viewModel.errorFlow.collect {

                val message = when {
                    it is PermissionChecker.PermissionsDeniedException &&
                            Manifest.permission.ACCESS_FINE_LOCATION in it.permissions &&
                            Manifest.permission.ACCESS_COARSE_LOCATION in it.permissions -> {
                        getString(R.string.location_permission_required)
                    }

                    it is LocationIsOffException -> {
                        getString(R.string.location_is_off)
                    }

                    else -> {
                        getString(R.string.error_occurred)
                    }
                }

                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
