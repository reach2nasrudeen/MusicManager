package ir.heydarii.musicmanager.features.albumdetails

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.orhanobut.logger.Logger
import com.squareup.picasso.Picasso
import ir.heydarii.musicmanager.R
import ir.heydarii.musicmanager.base.BaseActivity
import ir.heydarii.musicmanager.pojos.AlbumDatabaseEntity
import ir.heydarii.musicmanager.utils.Consts
import ir.heydarii.musicmanager.utils.Consts.Companion.IS_OFFLINE
import ir.heydarii.musicmanager.utils.ImageStorageManager
import ir.heydarii.musicmanager.utils.ViewNotifierEnums
import kotlinx.android.synthetic.main.activity_album_details.*
import kotlinx.android.synthetic.main.album_details_main_layout.*
import java.io.File


class AlbumDetailsActivity : BaseActivity() {

    private lateinit var viewModel: AlbumDetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_details)

        viewModel = ViewModelProviders.of(this).get(AlbumDetailsViewModel::class.java)

        showData()

        //subscribes to show the album data
        viewModel.getAlbumsResponse().observe(this, Observer {
            setImagesTexts(it)

            showTrackList(it.tracks)

        })


        //subscribes to react to loading and errors
        viewModel.getViewNotifier().observe(this, Observer {
            when (it) {
                ViewNotifierEnums.SHOW_LOADING -> {
                    Logger.d(progress.isShown)
                    if (progress.visibility != View.VISIBLE)
                        progress.visibility = View.VISIBLE
                }
                ViewNotifierEnums.HIDE_LOADING -> {
                    Logger.d(progress.isShown)
                    if (progress.visibility == View.VISIBLE)
                        progress.visibility = View.GONE
                }
                ViewNotifierEnums.SAVED_INTO_DB -> showSaveStatus()
                ViewNotifierEnums.REMOVED_FROM_DB -> btnSave.progress = 0f
                ViewNotifierEnums.EMPTY_STATE -> showEmptyState()
                ViewNotifierEnums.NOT_EMPTY -> hideEmptyState()
                ViewNotifierEnums.ERROR_GETTING_DATA -> showTryAgain()
                ViewNotifierEnums.ERROR_DATA_NOT_AVAILABLE -> showDataNotAvailable()
                ViewNotifierEnums.ERROR_REMOVING_DATA, ViewNotifierEnums.ERROR_SAVING_DATA -> showDbError()

                else -> throw IllegalStateException(getString(R.string.a_notifier_is_not_defined_in_the_when_block))

            }
        })

        //observes the viewModel to understand the state of btnSave for the first time activity starts
        viewModel.getAlbumExistenceResponse().observe(this, Observer {
            when (it) {
                true -> btnSave.progress = 1f
                false -> btnSave.progress = 0f
            }
        })

        //click listener for btnSave
        btnSave.setOnClickListener {

            //TODO : Provide this via Dagger
            val path = ImageStorageManager.saveToInternalStorage(
                this,
                imgAlbum.drawable.toBitmap(),
                intent.getStringExtra(Consts.ALBUM_NAME)
            )
            disableSaveButtonForASecond()
            viewModel.onClickedOnSaveButton(path)
        }
    }

    /**
     * Shows an error when something wrong happens while user is trying to save or remove some data from db
     */
    private fun showDbError() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(parentLayout, getString(R.string.album_not_saved), Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.try_again)) {
                val path = ImageStorageManager.saveToInternalStorage(
                    this,
                    imgAlbum.drawable.toBitmap(),
                    intent.getStringExtra(Consts.ALBUM_NAME)
                )
                viewModel.onClickedOnSaveButton(path)
            }.show()
    }

    /**
     * Shows an error whenever the album data is not available and user tries to save it
     * into the db. this is an IllegalState and in future can be replaced with
     * an exception
     */
    private fun showDataNotAvailable() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(parentLayout, getString(R.string.album_is_not_available), Snackbar.LENGTH_LONG).show()

    }

    /**
     * Shows try again button whenever an error accrues while receiving the albums data
     */
    private fun showTryAgain() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(parentLayout, getString(R.string.please_try_again), Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(R.string.try_again)) {
                showData()
            }.show()
    }

    /**
     * Disables the save button so the database have some time to perform the requested action
     */
    private fun disableSaveButtonForASecond() {
        btnSave.isEnabled = false
        Handler().postDelayed({
            btnSave.isEnabled = true
        }, 2000)
    }

    /**
     * Shows save animation
     */
    private fun showSaveStatus() {
        btnSave.playAnimation()
    }

    /**
     * Sets Albums general data in the view
     */
    private fun setImagesTexts(album: AlbumDatabaseEntity) {
        if (album.image.isNotEmpty())
            if (album.image.startsWith("http"))
                Picasso.get().load(album.image).placeholder(R.drawable.ic_album_placeholder).into(imgAlbum)
            else {
                val file = File(album.image)
                Picasso.get().load(file).placeholder(R.drawable.ic_album_placeholder).into(imgAlbum)
            }
        txtAlbumName.text = album.albumName
        txtArtistName.text = album.artistName
    }

    /**
     * Gets intents and asks viewModel to fetch the data
     */
    private fun showData() {
        val isOffline = intent.getBooleanExtra(IS_OFFLINE, false)
        val artistName = intent.getStringExtra(Consts.ARTIST_NAME)
        val albumName = intent.getStringExtra(Consts.ALBUM_NAME)

        viewModel.getAlbum(artistName, albumName, Consts.API_KEY, isOffline)

    }


    /**
     * Shows tracks in a recycler view
     */
    private fun showTrackList(tracks: List<String>) {
        recycler.adapter = TracksAdapter(tracks)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    /**
     * Hides the empty state animation
     */
    private fun hideEmptyState() {
        empty.visibility = View.GONE
        recycler.visibility = View.VISIBLE
    }

    /**
     * Shows the empty state animation
     */
    private fun showEmptyState() {
        empty.visibility = View.VISIBLE
        recycler.visibility = View.GONE
    }

}
