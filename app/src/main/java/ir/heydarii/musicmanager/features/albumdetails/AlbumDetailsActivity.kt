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
import ir.heydarii.musicmanager.base.BaseViewModelFactory
import ir.heydarii.musicmanager.base.di.DaggerDataRepositoryComponent
import ir.heydarii.musicmanager.features.albumdetails.di.DaggerImageStorageComponent
import ir.heydarii.musicmanager.pojos.AlbumDatabaseEntity
import ir.heydarii.musicmanager.utils.Consts
import ir.heydarii.musicmanager.utils.Consts.Companion.IS_OFFLINE
import ir.heydarii.musicmanager.utils.ViewNotifierEnums
import kotlinx.android.synthetic.main.activity_album_details.*
import kotlinx.android.synthetic.main.album_details_main_layout.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import java.io.File


class AlbumDetailsActivity : BaseActivity() {

    private lateinit var viewModel: AlbumDetailsViewModel
    private var albumName = ""
    private val imageStorageManager = DaggerImageStorageComponent.create()
    private val repository = DaggerDataRepositoryComponent.create().getDataRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album_details)

        val viewModelFactory = BaseViewModelFactory(repository)
        viewModel =
                ViewModelProviders.of(this, viewModelFactory).get(AlbumDetailsViewModel::class.java)

        initToolbar()

        showData()

        albumName = intent.getStringExtra(Consts.ALBUM_NAME)

        subscribeToViewModel()

        //click listener for btnSave
        btnSave.setOnClickListener {
            val path = saveImage()
            disableSaveButtonForASecond()
            viewModel.onClickedOnSaveButton(path)
        }
    }

    private fun initToolbar() {
        txtTitle.visibility = View.GONE
        imgBack.setOnClickListener {
            finish()
        }
    }

    private fun subscribeToViewModel() {

        //observes the viewModel to understand the state of btnSave for the first time activity starts
        viewModel.getAlbumExistenceResponse().observe(this, Observer {
            when (it) {
                true -> btnSave.progress = 1f
                false -> btnSave.progress = 0f
            }
        })

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
                ViewNotifierEnums.SAVED_INTO_DB -> showSaveAnimation()
                ViewNotifierEnums.REMOVED_FROM_DB -> {
                    removeImage(albumName)
                    btnSave.progress = 0f
                }
                ViewNotifierEnums.EMPTY_STATE -> showEmptyState()
                ViewNotifierEnums.NOT_EMPTY -> hideEmptyState()
                ViewNotifierEnums.ERROR_GETTING_DATA -> showTryAgain()
                ViewNotifierEnums.ERROR_DATA_NOT_AVAILABLE -> showDataNotAvailable()
                ViewNotifierEnums.ERROR_REMOVING_DATA, ViewNotifierEnums.ERROR_SAVING_DATA -> showDbError()

                else -> throw IllegalStateException(getString(R.string.a_notifier_is_not_defined_in_the_when_block))

            }
        })
    }

    private fun showDbError() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(parentLayout, getString(R.string.album_not_saved), Snackbar.LENGTH_LONG).setAction(getString(R.string.try_again)) {
                    val path = saveImage()
                    viewModel.onClickedOnSaveButton(path)
                }.show()
    }

    private fun showDataNotAvailable() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(
                parentLayout,
                getString(R.string.album_is_not_available),
                Snackbar.LENGTH_LONG
        ).show()

    }

    private fun showTryAgain() {
        val parentLayout = findViewById<View>(android.R.id.content)
        Snackbar.make(
                parentLayout,
                getString(R.string.please_try_again),
                Snackbar.LENGTH_INDEFINITE
        )
                .setAction(getString(R.string.try_again)) {
                    showData()
                }.show()
    }

    private fun disableSaveButtonForASecond() {
        btnSave.isEnabled = false
        Handler().postDelayed({
            btnSave.isEnabled = true
        }, 2000)
    }

    private fun showSaveAnimation() {
        btnSave.playAnimation()
    }

    private fun setImagesTexts(album: AlbumDatabaseEntity) {
        if (album.image.isNotEmpty())
            if (album.image.startsWith("http"))
                Picasso.get().load(album.image).placeholder(R.drawable.ic_album_placeholder).into(
                        imgAlbum
                )
            else {
                val file = File(album.image)
                Picasso.get().load(file).placeholder(R.drawable.ic_album_placeholder).into(imgAlbum)
            }
        txtAlbumName.text = album.albumName
        txtArtistName.text = album.artistName
    }

    private fun showData() {
        val isOffline = intent.getBooleanExtra(IS_OFFLINE, false)
        val artistName = intent.getStringExtra(Consts.ARTIST_NAME)
        val albumName = intent.getStringExtra(Consts.ALBUM_NAME)

        viewModel.getAlbum(artistName, albumName, Consts.API_KEY, isOffline)

    }


    private fun showTrackList(tracks: List<String>) {
        recycler.adapter = TracksAdapter(tracks)
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
    }

    private fun hideEmptyState() {
        empty.visibility = View.GONE
        recycler.visibility = View.VISIBLE
    }

    private fun showEmptyState() {
        empty.visibility = View.VISIBLE
        recycler.visibility = View.GONE
    }

    private fun saveImage(): String {
        return imageStorageManager.getImageStorageManager()
                .saveToInternalStorage(applicationContext, imgAlbum.drawable.toBitmap(), albumName)
    }

    private fun removeImage(path: String) {
        imageStorageManager.getImageStorageManager()
                .deleteImageFromInternalStorage(path)
    }

}
