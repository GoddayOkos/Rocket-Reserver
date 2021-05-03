package com.example.rocketreserver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.rocketreserver.databinding.LaunchDetailsFragmentBinding

class LaunchDetailsFragment : Fragment() {

    private lateinit var binding: LaunchDetailsFragmentBinding
    private val args: LaunchDetailsFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LaunchDetailsFragmentBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            binding.apply {
                bookButton.visibility = View.GONE
                bookProgressBar.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                error.visibility = View.GONE
            }

            val response = try {
                apolloClient(requireContext()).query(LaunchDetailsQuery(id = args.launchId)).await()
            } catch (e: ApolloException) {
                binding.apply {
                    progressBar.visibility = View.GONE
                    error.text = e.message
                    error.visibility = View.VISIBLE
                }
                return@launchWhenResumed
            }

            val launch = response.data?.launch
            if (launch == null || response.hasErrors()) {
                binding.apply {
                    progressBar.visibility = View.GONE
                    error.text = response.errors?.get(0)?.message
                    error.visibility = View.VISIBLE
                }
                return@launchWhenResumed
            }

            binding.apply {
                progressBar.visibility = View.GONE

                missionPatch.load(launch.mission?.missionPatch) {
                    placeholder(R.drawable.ic_placeholder)
                }
                site.text = launch.site
                missionName.text = launch.mission?.name
                val rocket = launch.rocket
                rocketName.text = "🚀 ${rocket?.name} ${rocket?.type}"
            }

            configureButton(launch.isBooked)
        }
    }

    private fun configureButton(isBooked: Boolean) {
        binding.apply {
            bookButton.visibility = View.VISIBLE
            bookProgressBar.visibility = View.GONE

            bookButton.text = if (isBooked) {
                getString(R.string.cancel)
            } else {
                getString(R.string.book_now)
            }

            bookButton.setOnClickListener {
                val context = context
                if (context != null && User.getToken(context) == null) {
                    findNavController().navigate(R.id.open_login)
                    return@setOnClickListener
                }

                binding.apply {
                    bookButton.visibility = View.INVISIBLE
                    bookProgressBar.visibility = View.VISIBLE
                }

                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    val mutation = if (isBooked) {
                        CancelTripMutation(id = args.launchId)
                    } else {
                        BookTripMutation(id = args.launchId)
                    }

                    val response = try {
                        apolloClient(requireContext()).mutate(mutation).await()
                    } catch (e: ApolloException) {
                        configureButton(isBooked)
                        return@launchWhenResumed
                    }

                    if (response.hasErrors()) {
                        configureButton(isBooked)
                        return@launchWhenResumed
                    }
                    configureButton(!isBooked)
                }
            }
        }
    }
}