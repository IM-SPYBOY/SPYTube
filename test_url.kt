import java.net.URL
fun main() {
    val base = URL("https://api.hicine.info/api/")
    val resolved = URL(base, "../rpc/search/Mortal%20Kombat%20II")
    println(resolved.toString())
}
