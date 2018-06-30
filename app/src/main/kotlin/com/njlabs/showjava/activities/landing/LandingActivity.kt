package com.njlabs.showjava.activities.landing

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import com.njlabs.showjava.Constants
import com.njlabs.showjava.R
import com.njlabs.showjava.activities.BaseActivity
import com.njlabs.showjava.activities.apps.AppsActivity
import com.njlabs.showjava.activities.explorer.navigator.NavigatorActivity
import com.njlabs.showjava.activities.filepicker.FilePickerActivity
import com.njlabs.showjava.activities.landing.adapters.HistoryListAdapter
import com.njlabs.showjava.models.SourceInfo
import com.nononsenseapps.filepicker.AbstractFilePickerActivity.EXTRA_START_PATH
import com.nononsenseapps.filepicker.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_landing.*
import timber.log.Timber




class LandingActivity : BaseActivity() {

    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var landingHandler: LandingHandler

    private var historyListAdapter: HistoryListAdapter? = null

    private var historyItems = ArrayList<SourceInfo>()

    override fun init(savedInstanceState: Bundle?) {
        setupLayout(R.layout.activity_landing)
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.drawerOpen,
            R.string.drawerClose
        )
        drawerLayout.addDrawerListener(drawerToggle)
        landingHandler = LandingHandler(context)
        setupFab()
        if (savedInstanceState != null) {
            val historyItems = savedInstanceState.getParcelableArrayList<SourceInfo>("historyItems")
            if (historyItems != null) {
                this.historyItems = historyItems
                setupList()
            }
        }
        //startActivity(Intent(context, NavigatorActivity::class.java))
    }

    public override fun onResume() {
        super.onResume()
        if (hasValidPermissions()) {
            populateHistory(true)
        }
    }

    private fun setupFab() {
        selectionFab.addOnMenuItemClickListener { _, _, itemId ->
            when (itemId) {
                R.id.action_pick_installed -> {
                    startActivity(
                        Intent(context, AppsActivity::class.java)
                    )
                }
                R.id.action_pick_sdcard -> {
                    pickFile()
                }
            }
        }
    }

    private fun pickFile() {
        val i = Intent(context, FilePickerActivity::class.java)
        i.putExtra(EXTRA_START_PATH, Environment.getExternalStorageDirectory())
        startActivityForResult(i, Constants.FILE_PICKER_RESULT)
    }

    override fun postPermissionsGrant() {
        populateHistory()
    }

    private fun populateHistory(resume: Boolean = false) {
        landingHandler.loadHistory()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.e(it) }
            .subscribe {
                historyItems = it
                if (resume) {
                    historyListAdapter?.updateData(historyItems)
                } else {
                    setupList()
                }

            }
    }

    private fun setListVisibility(isListVisible: Boolean = true) {
        val listGroupVisibility = if (isListVisible) View.VISIBLE else View.GONE
        val defaultGroupVisibility = if (isListVisible) View.GONE else View.VISIBLE
        historyListView.visibility = listGroupVisibility
        pickAppText.visibility = listGroupVisibility
        welcomeLayout.visibility = defaultGroupVisibility
    }


    private fun setupList() {
        if (historyItems.isEmpty()) {
            setListVisibility(false)
        } else {
            setListVisibility(true)
            historyListView.setHasFixedSize(true)
            historyListView.layoutManager = LinearLayoutManager(context)
            historyListAdapter = HistoryListAdapter(historyItems) { selectedHistoryItem ->
                val intent = Intent(context, NavigatorActivity::class.java)
                intent.putExtra("selectedApp", selectedHistoryItem)
                startActivity(intent)
            }
            historyListView.adapter = historyListAdapter
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putParcelableArrayList("historyItems", historyItems)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.FILE_PICKER_RESULT && resultCode == Activity.RESULT_OK) {
            data?.let {
                Utils.getSelectedFilesFromResult(it)
                    .map { Utils.getFileForUri(it) }
                    .forEach { Timber.d(it.absolutePath) }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

}