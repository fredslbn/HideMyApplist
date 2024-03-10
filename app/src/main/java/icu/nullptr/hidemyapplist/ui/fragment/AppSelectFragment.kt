package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import com.tsng.hidemyapplist.R
import com.tsng.hidemyapplist.databinding.FragmentAppSelectBinding
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.adapter.AppSelectAdapter
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import icu.nullptr.hidemyapplist.util.PackageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class AppSelectFragment : Fragment(R.layout.fragment_app_select) {

    private val binding by viewBinding<FragmentAppSelectBinding>()

    protected abstract val firstComparator: Comparator<String>
    protected abstract val adapter: AppSelectAdapter

    private var search = ""

    private fun handleBack(): Boolean {
        Log.e("back", "handleBack?", Throwable())
        val searchView = binding.toolbar.menu.findItem(R.id.menu_search).actionView as SearchView
        return if (searchView.isIconified) {
            onBack()
            true
        } else {
            search = ""
            applyFilter()
            searchView.isIconified = true
            false
        }
    }

    protected open fun onBack() {
        navController.navigateUp()
    }

    private fun applyFilter() {
        adapter.filter.filter(search)
    }

    private fun refreshList() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            withContext(Dispatchers.IO) {
                PackageHelper.sortList(firstComparator)
            }
            binding.swipeRefresh.isRefreshing = false
            applyFilter()
        }
    }

    private fun onMenuOptionSelected(item: MenuItem) {
        when (item.itemId) {
            R.id.menu_show_system -> {
                item.isChecked = !item.isChecked
                PrefManager.appFilter_showSystem = item.isChecked
            }
            R.id.menu_sort_by_label -> {
                item.isChecked = true
                PrefManager.appFilter_sortMethod = PrefManager.SortMethod.BY_LABEL
            }
            R.id.menu_sort_by_package_name -> {
                item.isChecked = true
                PrefManager.appFilter_sortMethod = PrefManager.SortMethod.BY_PACKAGE_NAME
            }
            R.id.menu_sort_by_install_time -> {
                item.isChecked = true
                PrefManager.appFilter_sortMethod = PrefManager.SortMethod.BY_INSTALL_TIME
            }
            R.id.menu_sort_by_update_time -> {
                item.isChecked = true
                PrefManager.appFilter_sortMethod = PrefManager.SortMethod.BY_UPDATE_TIME
            }
            R.id.menu_reverse_order -> {
                item.isChecked = !item.isChecked
                PrefManager.appFilter_reverseOrder = item.isChecked
            }
        }
        refreshList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { handleBack() }
        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.title_app_select),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { handleBack() },
            menuRes = R.menu.menu_app_list,
            onMenuOptionSelected = this::onMenuOptionSelected
        )
        with(binding.toolbar.menu) {
            val searchView = findItem(R.id.menu_search).actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    search = newText
                    applyFilter()
                    return true
                }
            })

            findItem(R.id.menu_show_system).isChecked = PrefManager.appFilter_showSystem
            when (PrefManager.appFilter_sortMethod) {
                PrefManager.SortMethod.BY_LABEL -> findItem(R.id.menu_sort_by_label).isChecked = true
                PrefManager.SortMethod.BY_PACKAGE_NAME -> findItem(R.id.menu_sort_by_package_name).isChecked = true
                PrefManager.SortMethod.BY_INSTALL_TIME -> findItem(R.id.menu_sort_by_install_time).isChecked = true
                PrefManager.SortMethod.BY_UPDATE_TIME -> findItem(R.id.menu_sort_by_update_time).isChecked = true
            }
            findItem(R.id.menu_reverse_order).isChecked = PrefManager.appFilter_reverseOrder
        }
        binding.list.layoutManager = LinearLayoutManager(context)
        binding.list.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener {
            refreshList()
        }

        lifecycleScope.launch {
            PackageHelper.lowerAppList.flowWithLifecycle(lifecycle)
                .collect {
                    refreshList()
                }
        }

        binding.root.keyPreImeCallback = { event ->
            if (event.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_BACK) {
                handleBack()
            } else false
        }
    }
}
