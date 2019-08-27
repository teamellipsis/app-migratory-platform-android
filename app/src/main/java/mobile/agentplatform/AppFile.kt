package mobile.agentplatform

import java.io.File
import java.net.URI

class AppFile : File {
    var isPointer: Boolean = false

    constructor(pathname: String?) : super(pathname)
    constructor(parent: String?, child: String?) : super(parent, child)
    constructor(parent: File?, child: String?) : super(parent, child)
    constructor(uri: URI?) : super(uri)
    constructor(file: File?) : super(file?.absolutePath)
    constructor(pathname: String?, isPointer: Boolean) : super(pathname) {
        this.isPointer = isPointer
    }

    override fun toString(): String {
        if (isPointer) {
            return ".."
        }
        return this.name
    }
}
