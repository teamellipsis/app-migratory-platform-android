package mobile.agentplatform

import java.lang.Integer.parseInt
import java.net.Inet4Address

class Encoder {
    companion object {
        private const val BASE_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ@?"

        /**
         * Encode IPv4 address and port into 8 character string
         *
         * @param   ipv4 IPv4 address
         * @param   port valid port
         * @return  encoded string
         */
        fun encodeIpv4(ipv4: String, port: String): String {
            var binaryStr = ""
            val ipv4Splitted = ipv4.split('.')

            ipv4Splitted.forEach {
                val ipPart = parseInt(it, 10).toString(2)
                val prefix = if (ipPart.length % 8 == 0) 0 else 8 - ipPart.length
                // TODO(Update kotlin to 1.3.50 and use repeat function)
                for (i in 0 until prefix) {
                    binaryStr += '0'
                }
                binaryStr += ipPart
            }

            val binaryPort = parseInt(port, 10).toString(2)
            val prefix = if (binaryPort.length % 16 == 0) 0 else 16 - binaryPort.length
            // TODO(Update kotlin to 1.3.50 and use repeat function)
            for (i in 0 until prefix) {
                binaryStr += '0'
            }
            binaryStr += binaryPort

            var encodedStr = ""
            for (i in 0 until binaryStr.length step 6) {
                val splittedBinaryStr = binaryStr.substring(i, i + 6)
                val character = BASE_CHARS[parseInt(splittedBinaryStr, 2)]
                encodedStr += character
            }

            return encodedStr
        }

        fun encodeIpv4(ipv4: String, port: Int) {
            encodeIpv4(ipv4, port.toString())
        }

        fun encodeIpv4(ipv4: Inet4Address, port: Int) {
            encodeIpv4(ipv4.hostAddress, port.toString())
        }

        /**
         * Decode 8 character encoded string into IPv4, port pair
         *
         * @param   encodedString string by this encoder
         * @return  Null if decoding failed. Else return pair of IPv4, port.
         */
        fun decodeIpv4(encodedString: String): Pair<String, Int>? {
            var decodeStr = ""
            for (i in 0 until encodedString.length) {
                val character = BASE_CHARS.indexOf(encodedString[i])
                if (character == -1) return null
                val binaryStr = character.toString(2)
                val prefix = if (binaryStr.length % 6 == 0) 0 else 6 - binaryStr.length
                // TODO(Update kotlin to 1.3.50 and use repeat function)
                for (i in 0 until prefix) {
                    decodeStr += '0'
                }
                decodeStr += binaryStr
            }

            val decodeStrIp = decodeStr.substring(0, 32)  // 255.255.255.255 => 4bytes => 8bits x 4
            val decodeStrPort = decodeStr.substring(32, 48) // 65535 => 2bytes => 16bits

            var ipv4 = mutableListOf<Int>()
            for (i in 0 until decodeStrIp.length step 8) {
                val splitedBinaryStr = decodeStrIp.substring(i, i + 8)
                ipv4.add(parseInt(splitedBinaryStr, 2))
            }

            return Pair(ipv4.joinToString(separator = "."), parseInt(decodeStrPort, 2))
        }
    }
}
