package AudioBB.temple.edu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class DisplayFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?){
        super .onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model= ViewModelProvider(requireActivity()).get(BookModel::class.java)


        val titleView = view.findViewById<TextView>(R.id.frgTitleView)
        val authorView = view.findViewById<TextView>(R.id.frgAuthorView)
        titleView.text = "Example Text"
        model.mangaTitle.observe(
            viewLifecycleOwner,
            { o -> titleView.text = o!!.toString() }
        )
        model.mangaAuthor.observe(
            viewLifecycleOwner,
            { o -> authorView.text = o!!.toString() }
        )
    }

}
