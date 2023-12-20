package dev.arbjerg.lavalink.client.protocol

import dev.arbjerg.lavalink.internal.toJsonElement
import dev.arbjerg.lavalink.protocol.v4.*
import kotlinx.serialization.json.JsonElement

/**
 * Helper class fo builder [Filters].
 */
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

    /**
     * Sets the filter volume. If you just want to change the volume, it is highly recommended to use [dev.arbjerg.lavalink.client.IUpdatablePlayer.setVolume] instead.
     *
     * This volume takes the time of your buffer size to apply and should only be used if any other filters would increase the overall volume too much.
     *
     * @param volume The volume to apply to the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setVolume(volume: Float) = apply {
        this.volume = volume.toOmissible()
    }

    /**
     * Set the equalizer bands on the player.
     *
     * @param equalizer Each band to set in the equalizer. Default gain is 1.0f.
     *
     * @return The updated builder, useful for chaining
     */
    fun setEqualizer(equalizer: List<Band>) = apply {
        this.equalizer = equalizer.toOmissible()
    }

    /**
     * Set a specific band on the equalizer.
     *
     * @param band The band to set.
     * @param gain The gain to apply to the band. Default gain is 1.0f.
     *
     * @return The updated builder, useful for chaining
     */
    @JvmOverloads
    fun setEqualizerBand(band: Int, gain: Float = 1.0F) = apply {
        val eq = this.equalizer
        val bandObj = Band(band, gain)

        if (eq.isPresent()) {
            val currEq = eq.value.toMutableList()
            val bandIndex = currEq.indexOfFirst { it.band == band }

            if (bandIndex > -1) {
                currEq[bandIndex] = bandObj
            } else {
                currEq.add(bandObj)
            }

            this.equalizer = currEq.toOmissible()
        } else {
            this.equalizer = listOf(bandObj).toOmissible()
        }
    }

    /**
     * Set the karaoke filter on the player.
     *
     * @param karaoke The karaoke filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setKaraoke(karaoke: Karaoke?) = apply {
        this.karaoke = Omissible.of(karaoke)
    }

    /**
     * Sets the timescale filter on the player.
     *
     * @param timescale The timescale filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setTimescale(timescale: Timescale?) = apply {
        this.timescale = Omissible.of(timescale)
    }

    /**
     * Sets the tremolo filter on the player.
     *
     * @param tremolo The tremolo filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setTremolo(tremolo: Tremolo?) = apply {
        this.tremolo = Omissible.of(tremolo)
    }

    /**
     * Sets the vibrato filter on the player.
     *
     * @param vibrato The vibrato filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setVibrato(vibrato: Vibrato?) = apply {
        this.vibrato = Omissible.of(vibrato)
    }

    /**
     * Sets the distortion filter on the player.
     *
     * @param distortion The distortion filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setDistortion(distortion: Distortion?) = apply {
        this.distortion = Omissible.of(distortion)
    }

    /**
     * Sets the rotation filter on the player.
     *
     * @param rotation The rotation filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setRotation(rotation: Rotation?) = apply {
        this.rotation = Omissible.of(rotation)
    }

    /**
     * Sets the channel mix filter on the player.
     *
     * @param channelMix The channel mix filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setChannelMix(channelMix: ChannelMix?) = apply {
        this.channelMix = Omissible.of(channelMix)
    }

    /**
     * Sets the low pass filter on the player.
     *
     * @param lowPass The low pass filter to apply to the player. Set to null to disable the filter.
     *
     * @return The updated builder, useful for chaining
     */
    fun setLowPass(lowPass: LowPass?) = apply {
        this.lowPass = Omissible.of(lowPass)
    }

    /**
     * Set custom filter data for a plugin.
     *
     * @param name the name of the plugin filter
     * @param filter the filter data, can be a custom class as it will be serialised to json
     * @return The updated builder, useful for chaining
     */
    fun setPluginFilter(name: String, filter: Any) = apply {
        pluginFilters[name] = toJsonElement(filter)
    }

    /**
     * Set custom filter data for a plugin.
     *
     * @param name the name of the plugin filter
     * @param filter kotlin [JsonElement] that holds the filter data.
     * @return The updated builder, useful for chaining
     */
    fun setPluginFilter(name: String, filter: JsonElement) = apply {
        pluginFilters[name] = filter
    }

    /**
     * Removes a plugin filter with the given name.
     *
     * @param name the name of the plugin filter
     * @return The updated builder, useful for chaining
     */
    fun removePluginFilter(name: String) = apply {
        pluginFilters.remove(name)
    }

    /**
     * Builds the [Filters] object with the current configuration.
     *
     * @return the [Filters] object with the current configuration
     */
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
