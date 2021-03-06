package com.example.rocketreserver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.rocketreserver.databinding.LaunchListFragmentBinding
import kotlinx.coroutines.channels.Channel
import kotlin.math.log

class LaunchListFragment : Fragment() {
    private lateinit var binding: LaunchListFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LaunchListFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val launches = mutableListOf<LaunchListQuery.Launch>()
        val adapter = LaunchListAdapter(launches)
        adapter.onItemClicked = {
            findNavController().navigate(
                LaunchListFragmentDirections.openLaunchDetails(it.id)
            )
        }

        binding.launches.also {
            it.layoutManager = LinearLayoutManager(requireContext())
            it.adapter = adapter
        }

        val channel = Channel<Unit>(Channel.CONFLATED)
        // offer a first item to do the initial load else the list will stay empty forever
        channel.offer(Unit)
        adapter.onEndOfListReached = {
            channel.offer(Unit)
        }

        lifecycleScope.launchWhenResumed {
            var cursor: String? = null
            for (item in channel) {
                val response = try {
                    apolloClient(requireContext()).query(LaunchListQuery(cursor = Input.fromNullable(cursor))).await()
                } catch (e: ApolloException) {
                    logIt("Failure $e")
                    return@launchWhenResumed
                }

                logIt(response.data?.toString()!!)
                val newLaunches = response.data?.launches?.launches?.filterNotNull()
                if (newLaunches != null) {
                    launches.addAll(newLaunches)
                    adapter.notifyDataSetChanged()
                }

                cursor = response.data?.launches?.cursor
                if (response.data?.launches?.hasMore != true) {
                    break
                }
            }

            adapter.onEndOfListReached = null
            channel.close()
        }
    }
}