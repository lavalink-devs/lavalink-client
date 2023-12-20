package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.toJsonElement
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.json.JsonElement

class FilterBuilder {
    private var volume: Omissible<Float> = Omissible.Omitted()
    private var equalizer: Omissible<List<Band>> = Omissible.Omitted()
    private var karaoke: Omissible<Karaoke?> = Omissible.Omitted()
    private var timescale: Omissible<Timescale?> = Omissible.Omitted()
    private var tremolo: Omissible<Tremolo?> = Omissible.Omitted()
    private var vibrato: Omissible<Vibrato?> = Omissible.Omitted()
    private var distortion: Omissible<Distortion?> = Omissible.Omitted()
    private var rotation: Omissible<Rotation?> = Omissible.Omitted()
    private var channelMix: Omissible<ChannelMix?> = Omissible.Omitted()
    private var lowPass: Omissible<LowPass?> = Omissible.Omitted()
    private var pluginFilters: MutableMap<String, JsonElement> = mutableMapOf()

    fun setVolume(volume: Float) = apply {
        this.volume = volume.toOmissible()
    }

    fun setEqualizer(equalizer: List<Band>) = apply {
        this.equalizer = equalizer.toOmissible()
    }

    fun setKaraoke(karaoke: Karaoke?) = apply {
        this.karaoke = Omissible.of(karaoke)
    }

    fun setTimescale(timescale: Timescale?) = apply {
        this.timescale = Omissible.of(timescale)
    }

    fun setTremolo(tremolo: Tremolo?) = apply {
        this.tremolo = Omissible.of(tremolo)
    }

    fun setVibrato(vibrato: Vibrato?) = apply {
        this.vibrato = Omissible.of(vibrato)
    }

    fun setDistortion(distortion: Distortion?) = apply {
        this.distortion = Omissible.of(distortion)
    }

    fun setRotation(rotation: Rotation?) = apply {
        this.rotation = Omissible.of(rotation)
    }

    fun setChannelMix(channelMix: ChannelMix?) = apply {
        this.channelMix = Omissible.of(channelMix)
    }

    fun setLowPass(lowPass: LowPass?) = apply {
        this.lowPass = Omissible.of(lowPass)
    }

    fun setPluginFilter(name: String, filter: Any) = apply {
        pluginFilters[name] = toJsonElement(filter)
    }

    fun setPluginFilter(name: String, filter: JsonElement) = apply {
        pluginFilters[name] = filter
    }

    fun removePluginFilter(name: String) = apply {
        pluginFilters.remove(name)
    }

    fun build() = Filters(
        volume,
        equalizer,
        karaoke,
        timescale,
        tremolo,
        vibrato,
        distortion,
        rotation,
        channelMix,
        lowPass,
        pluginFilters
    )
}
