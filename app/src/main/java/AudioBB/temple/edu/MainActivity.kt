package AudioBB.temple.edu

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val mangaAuthors = resources.getStringArray(R.array.authors)
        val mangaNames = resources.getStringArray(R.array.book_titles)
        val blist = BookList()
        for(i in mangaAuthors.indices){
            blist.add(Book(mangaNames[i],mangaAuthors[i]))
        }
        val d=this.resources.displayMetrics.densityDpi
        Log.d("myTag", d.toString())
        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction()
                .replace(R.id.FragContainer1, SelectionFragment.newInstance(mangaNames,mangaAuthors))
                .commit()
    }
}