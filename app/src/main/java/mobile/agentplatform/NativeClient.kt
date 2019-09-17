package mobile.agentplatform

class NativeClient {

    /**
     * Start separate Node.js process by forking process natively.
     *
     * @param   argv arguments to Node.js process
     * @return  forked child process pid. pid = -1 if failed to fork.
     */
    fun startNodeWithArgs(vararg argv: String): Int {
        return startNodeWithArguments(*argv)
    }

    private external fun startNodeWithArguments(vararg argv: String): Int

    companion object {
        init {
            try {
                System.loadLibrary("native-lib")
                System.loadLibrary("node")
            } catch (error: UnsatisfiedLinkError) {
                error.printStackTrace()
            }
        }
    }
}
