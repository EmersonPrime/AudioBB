package AudioBB.temple.edu

class BookList() {
    private val booklist= mutableListOf<Book>()

    fun add(b:Book){
        booklist.add(b)
    }
    fun remove(b:Book){
        booklist.remove(b)
    }
    fun get(i:Int): Book {
        return  booklist[(i)]
    }
    fun size():Int{
        return booklist.size
    }
    override fun toString():String{
        var output = ""
        for(i in booklist.indices){
            output += i.toString()
        }
        return output
    }

}