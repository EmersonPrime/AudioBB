package AudioBB.temple.edu

import android.content.*
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import edu.temple.audlibplayer.PlayerService
import java.io.*
import java.net.URL
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

class MainActivity : AppCompatActivity(), BookListFragment.BookSelectedInterface , ControlFragment.MediaControlInterface, Serializable{

    lateinit var progFile: File
    private lateinit var bookListFragment : BookListFragment
    private lateinit var serviceIntent : Intent
    private lateinit var mediaControlBinder : PlayerService.MediaControlBinder
    private var connected = false
    private var hashMap = HashMap<Int, Int>()

    val audiobookHandler = Handler(Looper.getMainLooper()) { msg ->

        // obj (BookProgress object) may be null if playback is paused
        msg.obj?.let { msgObj ->
            val bookProgress = msgObj as PlayerService.BookProgress
            // If the service is playing a book but the activity doesn't know about it
            // (this would happen if the activity was closed and then reopened) then
            // fetch the book details so the activity can be properly updated
            if (playingBookViewModel.getPlayingBook().value == null) {
                Volley.newRequestQueue(this)
                    .add(JsonObjectRequest(Request.Method.GET, API.getBookDataUrl(bookProgress.bookId), null, { jsonObject ->
                        playingBookViewModel.setPlayingBook(Book(jsonObject))
                        // If no book is selected (if activity was closed and restarted)
                        // then use the currently playing book as the selected book.
                        // This allows the UI to display the book details
                        if (selectedBookViewModel.getSelectedBook().value == null) {
                            // set book
                            selectedBookViewModel.setSelectedBook(playingBookViewModel.getPlayingBook().value)
                            // display book - this function was previously implemented as a callback for
                            // the BookListFragment, but it turns out we can use it here - Don't Repeat Yourself
                            bookSelected()
                        }
                    }, {}))
            }

            hashMap[selectedBookViewModel.getSelectedBook().value!!.id] = bookProgress.progress

            // Everything that follows is to prevent possible NullPointerExceptions that can occur
            // when the activity first loads (after config change or opening after closing)
            // since the service can (and will) send updates via the handler before the activity fully
            // loads, the currently playing book is downloaded, and all variables have been initialized
            supportFragmentManager.findFragmentById(R.id.controlFragmentContainerView)?.run{
                with (this as ControlFragment) {
                    playingBookViewModel.getPlayingBook().value?.also {

                        val progress = ((bookProgress.progress / it.duration.toFloat()) * 100).toInt()
                        setPlayProgress(progress)
                    }
                }
            }
        }

        true
    }

    private val searchRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        supportFragmentManager.popBackStack()
        it.data?.run {
            val results = getSerializableExtra(BookList.BOOKLIST_KEY) as BookList
            bookListViewModel.copyBooks(results)
            bookListFragment.bookListUpdated()

            val progress = getSharedPreferences(progFile.name, Context.MODE_PRIVATE)
            val editor = progress.edit()

            editor.putString("previous_search_results", results.toString())
        }
    }

    private val serviceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mediaControlBinder = service as PlayerService.MediaControlBinder
            mediaControlBinder.setProgressHandler(audiobookHandler)
            connected = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            connected = false
        }

    }

    private val isSingleContainer : Boolean by lazy{
        findViewById<View>(R.id.container2) == null
    }

    private val selectedBookViewModel : SelectedBookViewModel by lazy {
        ViewModelProvider(this).get(SelectedBookViewModel::class.java)
    }

    private val playingBookViewModel : PlayingBookViewModel by lazy {
        ViewModelProvider(this).get(PlayingBookViewModel::class.java)
    }

    private val bookListViewModel : BookList by lazy {
        ViewModelProvider(this).get(BookList::class.java)
    }

    companion object {
        const val BOOKLISTFRAGMENT_KEY = "BookListFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Checks if file exists, if not, create one
        progFile = File(filesDir, "Progress")
        if(!progFile.exists()){
            progFile.createNewFile()
        }

        playingBookViewModel.getPlayingBook().observe(this, {
            (supportFragmentManager.findFragmentById(R.id.controlFragmentContainerView) as ControlFragment).setNowPlaying(it.title)
        })

        // Create intent for binding and starting service
        serviceIntent = Intent(this, PlayerService::class.java)

        // bind to service
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)

        // If we're switching from one container to two containers
        // clear BookDetailsFragment from container1
        if (supportFragmentManager.findFragmentById(R.id.container1) is BookDetailsFragment
            && selectedBookViewModel.getSelectedBook().value != null) {
            supportFragmentManager.popBackStack()
        }

        // If this is the first time the activity is loading, go ahead and add a BookListFragment
        if (savedInstanceState == null) {
            bookListFragment = BookListFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.container1, bookListFragment, BOOKLISTFRAGMENT_KEY)
                .commit()
        } else {
            bookListFragment = supportFragmentManager.findFragmentByTag(BOOKLISTFRAGMENT_KEY) as BookListFragment
            // If activity loaded previously, there's already a BookListFragment
            // If we have a single container and a selected book, place it on top
            if (isSingleContainer && selectedBookViewModel.getSelectedBook().value != null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container1, BookDetailsFragment())
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // If we have two containers but no BookDetailsFragment, add one to container2
        if (!isSingleContainer && supportFragmentManager.findFragmentById(R.id.container2) !is BookDetailsFragment)
            supportFragmentManager.beginTransaction()
                .add(R.id.container2, BookDetailsFragment())
                .commit()

        findViewById<ImageButton>(R.id.searchButton).setOnClickListener {
            searchRequest.launch(Intent(this, SearchActivity::class.java))
        }

    }

    override fun onBackPressed() {
        // Back press clears the selected book and updates shared preferences

        updateSharedPreferences()
        selectedBookViewModel.setSelectedBook(null)
        super.onBackPressed()
    }

    override fun bookSelected() {
        // Perform a fragment replacement if we only have a single container
        // when a book is selected

        if (isSingleContainer) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container1, BookDetailsFragment())
                .setReorderingAllowed(true)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun play() {
        // Uses sharedpreferences
        if (connected && selectedBookViewModel.getSelectedBook().value != null) {

            val sharedPref = getSharedPreferences(progFile.name, Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            // Plays back audiobook
            editor.putInt("currently_playing", selectedBookViewModel.getSelectedBook().value!!.id)
            editor.apply()
            Toast.makeText(this, "Playing book", Toast.LENGTH_SHORT).show()

            val selectedBook = selectedBookViewModel.getSelectedBook().value
            val selectedBookUrl = API.getBookDataUrl(selectedBook!!.id)

            if(!(hashMap.containsKey(selectedBook.id))) {
                val sharedPreferences = getSharedPreferences(progFile.name, Context.MODE_PRIVATE)
                val time = sharedPreferences.getInt(selectedBook.id.toString(), 0)
                hashMap[selectedBook.id] = time
            }

            // If file is stored internally on the system, it is played, otherwise it is downloaded
            if(fileExists("${selectedBook.id}.mp3")){
                mediaControlBinder.play(File(filesDir, "${selectedBook.id}.mp3"), hashMap[selectedBook.id]!!)
                Toast.makeText(this, "File exists, playing...", Toast.LENGTH_SHORT).show()
            }
            else{
                mediaControlBinder.seekTo(hashMap[selectedBook.id]!!)
                mediaControlBinder.play(selectedBook.id)
                DownloadAudioBook(this, selectedBook.id.toString()).execute(selectedBookUrl)
                Toast.makeText(this, "File not stored internally, downloading...", Toast.LENGTH_SHORT).show()
            }

            playingBookViewModel.setPlayingBook(selectedBook)
            startService(serviceIntent)
        }
    }

    override fun pause() {
        if (connected) {
            mediaControlBinder.pause()
        }
    }

    override fun stop() {
        if (connected) {
            hashMap[selectedBookViewModel.getSelectedBook().value!!.id] = 0
            mediaControlBinder.stop()
            stopService(serviceIntent)
        }
    }

    override fun seek(position: Int) {
        // Converting percentage to proper book progress
        if (connected) mediaControlBinder.seekTo((playingBookViewModel.getPlayingBook().value!!.duration * (position.toFloat() / 100)).toInt())
    }

    override fun onDestroy() {
        updateSharedPreferences()
        super.onDestroy()
        unbindService(serviceConnection)
    }

    private fun fileExists(fileName: String): Boolean {
        val filePath: String = this.filesDir.absolutePath.toString() + "/" + fileName
        val file = File(filePath)
        return file.exists()
    }

    private fun updateSharedPreferences(){
        val sharedPref = getSharedPreferences(progFile.name, Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        for(i in hashMap.keys){
            editor.putInt(i.toString(), hashMap[i]!!)
        }
        editor.apply()
    }

    inner class DownloadAudioBook(val context: Context, id: String): AsyncTask<String, String, String>() {

        private var download = id

        override fun doInBackground(vararg url_arg: String?): String {
            val url  = URL(url_arg[0])
            url.openConnection().connect()

            // Next line creates an input buffer
            val dataBuffer = ByteArray(1024)
            val fileName = "${this.download}.mp3"
            val inputStream = BufferedInputStream(url.openStream())
            val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)

            var downloaded = 0

            // Cycles through inputstream and writes to outputstream until the end of the inputstream
            while (inputStream.read() != -1) {
                outputStream.write(dataBuffer)
                downloaded += dataBuffer.size
            }

            outputStream.flush()
            inputStream.close()
            outputStream.close()

            return "Data successfully written to output stream"
        }
    }
}