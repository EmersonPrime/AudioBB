package AudioBB.temple.edu

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "mangaDescription"
private const val ARG_PARAM2 = "mangaImage"

/**
 * A simple [Fragment] subclass.
 * Use the [SelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SelectionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: Array<String>? = null
    private var param2: Array<String>? = null
    private lateinit var displayFragment: DisplayFragment



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getStringArray(ARG_PARAM1)
            param2 = it.getStringArray(ARG_PARAM2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_selection, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recycle = view.findViewById<RecyclerView>(R.id.rcvFragView)
        val model= ViewModelProvider(requireActivity()).get(BookModel::class.java)
        val size = param1?.size
        Log.d("SIZE", size.toString())
        if (size != null) {
            val mangaData = BookList()
            for (i in 0 until size) {
                mangaData.add( Book(param1!![i],param2!![i]))
            }
            val adapter = BookAdapter(mangaData)
            val height =requireActivity().resources.displayMetrics.heightPixels
            val width = requireActivity().resources.displayMetrics.widthPixels
            recycle.adapter = adapter
            displayFragment = DisplayFragment()
            adapter.setOnItemClickListener(object : BookAdapter.OnItemClickListener {
                override fun onItemClick(position: Int) {
                    model.mangaTitle(mangaData.get(position).title)
                    model.mangaAuthor(mangaData.get(position).author)
                    val d=activity!!.resources.displayMetrics.densityDpi
                    if (activity!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT && d !=320){
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.FragContainer1,displayFragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            })
            recycle.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.VERTICAL,false)
        }

    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SelectionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: Array<String>, param2: Array<String>) =
            SelectionFragment().apply {
                arguments = Bundle().apply {
                    putStringArray(ARG_PARAM1,param1)
                    putStringArray(ARG_PARAM2, param2)
                }
            }
    }
}