import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * An "unsafe" helper method to return the user id from a discord bot token
 */
@OptIn(ExperimentalEncodingApi::class)
fun userIdFromToken(token: String) = String(Base64.decode(token.split("\\.")[0])).toLong()
