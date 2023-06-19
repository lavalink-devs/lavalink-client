package dev.arbjerg.lavalink.internal.error

import dev.arbjerg.lavalink.protocol.v4.Error

class RestException(val error: Error) : Exception(error.message)
