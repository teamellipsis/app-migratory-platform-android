package mobile.agentplatform

interface MainFragmentInteractionListener {
    fun onFragmentChange(itemId: Int)
    fun onFragmentChange(itemId: Int, extra: String?)
}
