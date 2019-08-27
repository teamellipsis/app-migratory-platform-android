package mobile.agentplatform

class NativeClient {

    fun startNodeWithArgs(vararg argv: String): Integer {
        return startNodeWithArguments(*argv)
    }

    private external fun startNodeWithArguments(vararg argv: String): Integer

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
