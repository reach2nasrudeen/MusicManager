package ir.heydarii.musicmanager.features.topalbums

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import ir.heydarii.musicmanager.R
import ir.heydarii.musicmanager.base.BaseFragment
import ir.heydarii.musicmanager.base.BaseViewModelFactory
import ir.heydarii.musicmanager.base.di.DaggerDataRepositoryComponent
import ir.heydarii.musicmanager.features.albumdetails.AlbumDetailsActivity
import ir.heydarii.musicmanager.pojos.Album
import ir.heydarii.musicmanager.utils.Consts
import ir.heydarii.musicmanager.utils.Consts.Companion.ALBUM_NAME
import ir.heydarii.musicmanager.utils.Consts.Companion.ARTIST_NAME
import ir.heydarii.musicmanager.utils.ViewNotifierEnums
import kotlinx.android.synthetic.main.activity_top_albums.*
import kotlinx.android.synthetic.main.toolbar_layout.*

/**
 * Shows top albums of an artist
 */
class TopAlbumsFragment : BaseFragment() {

    lateinit var viewModel: TopAlbumsViewModel
    private lateinit var adapter: TopAlbumsAdapter
    private val albumDataList = ArrayList<Album>()
    private val repository = DaggerDataRepositoryComponent.create().getDataRepository()


    /**
     * Inflating the view
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.activity_top_albums, container, false)
    }


    /**
     * All codes are here
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModelFactory = BaseViewModelFactory(repository)

        viewModel =
                ViewModelProviders.of(this, viewModelFactory).get(TopAlbumsViewModel::class.java)

        initToolbar()

        setUpRecycler()

        showData(savedInstanceState)

        //subscribes to get the albums data
        viewModel.getTopAlbumsLiveData().observe(this, Observer {
            showList(it)
        })

        //subscribes to show or hide loading
        viewModel.getViewNotifier().observe(this, Observer {
            when (it) {
                ViewNotifierEnums.SHOW_LOADING -> progress.visibility = View.VISIBLE
                ViewNotifierEnums.HIDE_LOADING -> progress.visibility = View.INVISIBLE
                ViewNotifierEnums.ERROR_GETTING_DATA -> showTryAgain()
                else -> throw java.lang.IllegalStateException(getString(R.string.a_notifier_is_not_defined_in_the_when_block))

            }
        })
    }

    private fun initToolbar() {
        txtTitle.text = getString(R.string.top_albums)

        imgBack.setOnClickListener {
            activity!!.onBackPressed()
        }
    }

    private fun setUpRecycler() {

        adapter = TopAlbumsAdapter(albumDataList) { artistName, albumName ->
            showAlbumDetailsView(artistName, albumName)
        }
        recycler.adapter = adapter
        val layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recycler.layoutManager = layoutManager

        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lastItem = layoutManager.findLastVisibleItemPosition()
                val total = layoutManager.itemCount
                if (total > 0)
                    if (total - 1 == lastItem)
                        viewModel.onTopAlbumsRequested(apiKey = Consts.API_KEY)
            }
        })
    }

    private fun showTryAgain() {
        Snackbar.make(rootView, getString(R.string.please_try_again), Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.try_again)) {
                    showData(null)
                }.show()
    }

    private fun showData(savedInstanceState: Bundle?) {

        val artistName = arguments?.let {
            TopAlbumsFragmentArgs.fromBundle(it).artistName
        }

        if (!artistName.isNullOrEmpty()) {
            showArtistName(artistName)

            //don't request again to get data after rotation
            if (savedInstanceState == null)
                viewModel.onTopAlbumsRequested(artistName, Consts.API_KEY)
        } else
            throw IllegalStateException("You have to pass the artist Name")

    }

    private fun showArtistName(artistName: String) {
        txtName.text = artistName
    }

    private fun showList(albumsData: List<Album>) {
        albumDataList.clear()
        albumDataList.addAll(albumsData)
        adapter.notifyDataSetChanged()
    }

    private fun showAlbumDetailsView(artistName: String, albumName: String) {
        val intent = Intent(activity!!, AlbumDetailsActivity::class.java)
        intent.putExtra(ARTIST_NAME, artistName)
        intent.putExtra(ALBUM_NAME, albumName)
        startActivity(intent)
    }

}