package com.zest.android.ui.recipes

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.zest.android.R
import com.zest.android.RecipesDirections
import com.zest.android.data.model.Recipe
import com.zest.android.databinding.FragmentRecipesBinding
import com.zest.android.ui.category.CategoryFragment
import com.zest.android.ui.main.MainActivity
import com.zest.android.ui.main.OnMainCallback
import com.zest.android.util.NetworkStateReceiver
import com.zest.android.util.currentNavigationFragment
import javax.inject.Inject
import javax.inject.Provider

/**
 * Display a grid of [Recipe]s. User can choose to view each recipe.
 *
 * Created by ZARA
 */
class RecipesFragment : Fragment(), NetworkStateReceiver.OnNetworkStateReceiverListener,
        OnRecipesFragmentInteractionListener {


    private var mAdapter = RecipesAdapter(this)
    private var mCallback: OnMainCallback? = null
    private var mNetworkReceiver = NetworkStateReceiver()
    private lateinit var binding: FragmentRecipesBinding
    private lateinit var viewModel: RecipeViewModel

    @Inject
    lateinit var viewModelProvider: Provider<RecipeViewModel>


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnMainCallback) {
            mCallback = context
        } else {
            throw ClassCastException("$context must implement OnMainCallback!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        (activity as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        (activity as MainActivity).mainComponent.inject(this)

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T = viewModelProvider.get() as T
        }).get(RecipeViewModel::class.java)


        mNetworkReceiver.addListener(this)
        context?.registerReceiver(mNetworkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRecipesBinding.inflate(inflater)
        binding.recipesRecyclerView.adapter = mAdapter

        with(viewModel) {
            getMainRecipes()

            recipesData.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                mAdapter.recipes = it
            })

            isLoading.observe(viewLifecycleOwner, Observer {
                binding.recipesProgressBar.visibility = if (it == true) View.VISIBLE else View.GONE
            })
        }

        mAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onChanged() {
                super.onChanged()
                checkEmptyView()
            }
        })

        return binding.root
    }


    override fun showMessage(message: Int) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).setAction("Action", null).show()
    }

    override fun gotoDetailPage(recipe: Recipe) {
        findNavController().navigate(RecipesFragmentDirections.actionRecipesFragmentToDetailFragment(recipe))
    }

    override fun loadFavorite(recipe: Recipe): Recipe? {
        return viewModel.loadFavorite(recipe)
    }

    override fun removeFavorite(recipe: Recipe) {
        viewModel.deleteFavoriteFromDB(recipe)
    }

    override fun addOrRemoveFavorites(recipe: Recipe) {
        viewModel.addOrRemoveAsFavorite(recipe)
        mAdapter.notifyDataSetChanged()
    }

    override fun isFavorited(recipe: Recipe): Boolean {
        return viewModel.isFavorited(recipe)
    }

    override fun networkAvailable() {
        viewModel.getMainRecipes()
    }

    override fun networkUnavailable() {
        //nothing yet
    }

    private fun checkEmptyView() {
        binding.recipesEmptyContainer.visibility = if (mAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.recipes, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                gotoSearchPage()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun gotoSearchPage() {
        if (findNavController().currentDestination?.id == R.id.recipesFragment) {
            findNavController().navigate(R.id.action_recipesFragment_to_searchFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mNetworkReceiver.removeListener(this)
        unregisterNetworkChanges()
    }

    private fun unregisterNetworkChanges() {
        context?.unregisterReceiver(mNetworkReceiver)
    }


    override fun onDetach() {
        super.onDetach()
        mCallback = null
    }

    companion object {

        private val TAG = RecipesFragment::class.java.name
    }
}
