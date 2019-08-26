package mobile.agentplatform

class AgentRules {
    companion object {
        const val NEXT = ".next"
        const val NEXT_BUILD_ID = "BUILD_ID"
        const val NEXT_BUILD_MANIFEST = "build-manifest.json"
        const val PACKAGE_LOCK = "package-lock.json"
        const val MAIN_JS = "server.js"

        const val RULE_COUNT = 4

        fun checkRequiredFiles(files:MutableList<String>):Boolean {
            var count = 0
            for (file in files){
                when {
                    file.contains("$NEXT/$NEXT_BUILD_ID") -> count++
                    file.contains("$NEXT/$NEXT_BUILD_MANIFEST") -> count++
                    file.contains(PACKAGE_LOCK) -> count++
                    file.contains(MAIN_JS) -> count++
                }

                if (count == RULE_COUNT) return true
            }

            return false
        }
    }
}
