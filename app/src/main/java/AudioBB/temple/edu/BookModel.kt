package AudioBB.temple.edu

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BookModel: ViewModel() {
    val mangaTitle: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }
    fun mangaTitle(item:String){
        mangaTitle.value=item
    }

    val mangaAuthor: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }
    fun mangaAuthor(item:String){
        mangaAuthor.value=item
    }
}