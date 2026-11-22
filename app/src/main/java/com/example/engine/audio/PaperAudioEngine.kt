package com.example.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.model.AmbientSoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/**
 * 100% procedural audio & micro-haptics synthesizer (0 KB media assets).
 * - Synthesizes soft, velocity-sensitive paper-rustle audio on AudioTrack (44.1 kHz).
 * - Synthesizes clear, audible, smooth looping Cozy Ambient Sounds (Soft Rain & Warm Vinyl Crackle)
 *   at 44.1 kHz with WRITE_BLOCKING and live volume updates.
 * - Synthesizes a gentle procedural completion bell chime + subtle double vibration for the Reading Timer.
 */
class PaperAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Streaming cozy ambient synthesizer state
    private var ambientJob: Job? = null
    @Volatile
    private var ambientGain: Float = 0.55f
    @Volatile
    private var ambientFadeMultiplier: Float = 1.0f
    @Volatile
    private var currentAmbientMode: AmbientSoundMode = AmbientSoundMode.OFF
    @Volatile
    private var activeAmbientTrack: AudioTrack? = null

    fun playPageRustle(velocityNormalized: Float = 0.7f, stiffness: Float = 0.55f) {
        val vel = velocityNormalized.coerceIn(0.35f, 1.45f)
        val stiff = stiffness.coerceIn(0.2f, 1.0f)
        scope.launch {
            runCatching {
                val sampleRate = 44100
                val durationSec = (0.24f - (vel - 0.35f) * 0.05f).coerceIn(0.14f, 0.26f)
                val sampleCount = (sampleRate * durationSec).toInt()
                val pcm = ShortArray(sampleCount)

                val hpCutoff = 850f + stiff * 550f + vel * 220f
                val lpCutoff = 2700f + stiff * 1200f + vel * 450f
                val dt = 1f / sampleRate
                val rcHp = 1f / (2f * PI.toFloat() * hpCutoff)
                val alphaHp = rcHp / (rcHp + dt)
                val rcLp = 1f / (2f * PI.toFloat() * lpCutoff)
                val alphaLp = dt / (rcLp + dt)

                val rng = Random(System.nanoTime())
                var prevRaw = 0f
                var prevHp = 0f
                var prevLp = 0f

                val masterGain = (0.20f + vel * 0.10f).coerceAtMost(0.34f)
                val flutterFreq = 19f + rng.nextFloat() * 9f

                for (i in 0 until sampleCount) {
                    val t = i.toFloat() / sampleCount
                    val white = (rng.nextFloat() * 2f - 1f + (rng.nextFloat() * 2f - 1f)) * 0.5f

                    val hp = alphaHp * (prevHp + white - prevRaw)
                    prevRaw = white
                    prevHp = hp
                    val bp = prevLp + alphaLp * (hp - prevLp)
                    prevLp = bp

                    val attack = if (t < 0.16f) sin((t / 0.16f) * (PI / 2.0)).toFloat() else 1f
                    val primaryDecay = exp((-3.6f * (t - 0.12f).coerceAtLeast(0f)).toDouble()).toFloat()
                    val settleSwell = 0.28f * exp((-36.0 * (t - 0.68f) * (t - 0.68f))).toFloat()
                    val crinkle = 0.86f + 0.14f * sin(2f * PI.toFloat() * flutterFreq * t * durationSec)

                    val env = (attack * primaryDecay + settleSwell) * crinkle
                    val sample = (bp * env * masterGain * 32767f).toInt().coerceIn(-32767, 32767)
                    pcm[i] = sample.toShort()
                }

                val byteCount = sampleCount * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.write(pcm, 0, sampleCount)
                    track.play()
                    Thread.sleep((durationSec * 1000f).toLong() + 50L)
                }
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    /**
     * Gentle two-note harmonic bell chime synthesized procedurally when the Reading Session Timer completes.
     */
    fun playGentleCompletionChime() {
        scope.launch {
            runCatching {
                val sampleRate = 44100
                val durationSec = 1.45f
                val sampleCount = (sampleRate * durationSec).toInt()
                val pcm = ShortArray(sampleCount)

                // Soft warm C5 -> G5 harmonic chime
                val f1 = 523.25
                val f2 = 783.99
                val secondStart = (sampleRate * 0.24f).toInt()

                for (i in 0 until sampleCount) {
                    val t1 = i.toDouble() / sampleRate
                    val attack1 = (t1 / 0.018).coerceAtMost(1.0)
                    val decay1 = exp(-3.4 * t1)
                    val note1 = (
                        sin(2.0 * PI * f1 * t1) * 0.68 +
                            sin(2.0 * PI * f1 * 2.0 * t1) * 0.22 +
                            sin(2.0 * PI * f1 * 3.0 * t1) * 0.08
                        ) * attack1 * decay1

                    var note2 = 0.0
                    if (i >= secondStart) {
                        val t2 = (i - secondStart).toDouble() / sampleRate
                        val attack2 = (t2 / 0.020).coerceAtMost(1.0)
                        val decay2 = exp(-2.8 * t2)
                        note2 = (
                            sin(2.0 * PI * f2 * t2) * 0.65 +
                                sin(2.0 * PI * f2 * 2.0 * t2) * 0.20 +
                                sin(2.0 * PI * f2 * 3.0 * t2) * 0.07
                            ) * attack2 * decay2
                    }

                    val mixed = tanh((note1 + note2) * 0.55) * 0.72
                    pcm[i] = (mixed * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                }

                val byteCount = sampleCount * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(byteCount, minBuf))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.write(pcm, 0, sampleCount)
                    track.play()
                    Thread.sleep((durationSec * 1000f).toLong() + 80L)
                }
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    fun setAmbientSound(mode: AmbientSoundMode, volume: Float) {
        ambientGain = volume.coerceIn(0f, 1f)
        ambientFadeMultiplier = 1.0f
        if (mode == AmbientSoundMode.OFF) {
            stopAmbient()
            return
        }
        if (currentAmbientMode == mode && ambientJob?.isActive == true) {
            applyTrackVolume()
            return
        }
        stopAmbient()
        currentAmbientMode = mode

        ambientJob = scope.launch {
            val sampleRate = 44100
            val chunkSamples = 4096
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(8192)
            val bufferBytes = maxOf(minBuf * 2, chunkSamples * 4)

            var track: AudioTrack? = null
            runCatching {
                val created = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferBytes)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (created.state != AudioTrack.STATE_INITIALIZED) {
                    created.release()
                    return@runCatching
                }
                track = created
                activeAmbientTrack = created
                applyTrackVolume()

                val pcm = ShortArray(chunkSamples)
                val rng = Random(System.nanoTime())

                // Paul Kellet pink-noise filter states for rich, warm, audible rainfall
                var b0 = 0f
                var b1 = 0f
                var b2 = 0f
                var b3 = 0f
                var b4 = 0f
                var b5 = 0f
                var b6 = 0f

                // Secondary smoothing & crackle states
                var lpRain = 0f
                var lpVinyl = 0f
                var hpVinylPrev = 0f
                var hpVinylOut = 0f
                var popDecay = 0f
                var popSign = 1f
                var swellPhase = 0.0
                var humPhase = 0.0
                var started = false

                while (isActive && currentAmbientMode == mode) {
                    val effectiveGain = (ambientGain * ambientFadeMultiplier).coerceIn(0f, 1f)
                    if (mode == AmbientSoundMode.RAIN) {
                        for (i in 0 until chunkSamples) {
                            val white = rng.nextFloat() * 2f - 1f
                            b0 = 0.99886f * b0 + white * 0.0555179f
                            b1 = 0.99332f * b1 + white * 0.0750759f
                            b2 = 0.96900f * b2 + white * 0.1538520f
                            b3 = 0.86650f * b3 + white * 0.3104856f
                            b4 = 0.55000f * b4 + white * 0.5329522f
                            b5 = -0.7616f * b5 - white * 0.0168980f
                            val pink = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362f) * 0.14f
                            b6 = white * 0.115926f

                            // Warm roof-rain lowpass bed + gentle droplet patter
                            lpRain += 0.11f * (pink - lpRain)
                            val swell = 0.84f + 0.16f * sin(swellPhase).toFloat()
                            swellPhase += (2.0 * PI * 0.12) / sampleRate

                            var drop = 0f
                            if (rng.nextInt(1400) == 0) {
                                popDecay = 0.55f + rng.nextFloat() * 0.35f
                            }
                            if (popDecay > 0.002f) {
                                drop = (rng.nextFloat() * 2f - 1f) * popDecay * 0.38f
                                popDecay *= 0.965f
                            }

                            val rawSample = (lpRain * 1.15f + pink * 0.58f + drop) * swell
                            val shaped = tanh((rawSample * (0.35f + effectiveGain * 0.95f)).toDouble()).toFloat() * 0.68f
                            pcm[i] = (shaped * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    } else {
                        // Warm Vinyl Crackle: cozy analog surface hiss, gentle 55Hz turntable warmth, organic crackles & pops
                        for (i in 0 until chunkSamples) {
                            val white = rng.nextFloat() * 2f - 1f
                            lpVinyl += 0.08f * (white - lpVinyl)

                            // Gentle high-pass for delicate surface dust hiss
                            val hp = 0.92f * (hpVinylOut + white - hpVinylPrev)
                            hpVinylPrev = white
                            hpVinylOut = hp

                            val warmHum = 0.055f * sin(humPhase).toFloat() + 0.025f * sin(humPhase * 2.0).toFloat()
                            humPhase += (2.0 * PI * 55.0) / sampleRate

                            var crackle = 0f
                            val roll = rng.nextInt(4200)
                            if (roll == 0) {
                                popDecay = 0.85f + rng.nextFloat() * 0.35f
                                popSign = if (rng.nextBoolean()) 1f else -1f
                            } else if (roll < 7) {
                                crackle = (rng.nextFloat() * 2f - 1f) * 0.48f
                            }
                            if (popDecay > 0.004f) {
                                crackle += popSign * popDecay * (0.65f + 0.35f * rng.nextFloat())
                                popDecay *= 0.86f
                            }

                            val rawSample = lpVinyl * 0.48f + hp * 0.14f + warmHum + crackle * 0.82f
                            val shaped = tanh((rawSample * (0.38f + effectiveGain * 0.98f)).toDouble()).toFloat() * 0.66f
                            pcm[i] = (shaped * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }

                    val written = created.write(pcm, 0, chunkSamples, AudioTrack.WRITE_BLOCKING)
                    if (!started && written > 0) {
                        created.play()
                        started = true
                    }
                    if (written < 0) break
                }
            }
            runCatching {
                if (activeAmbientTrack === track) {
                    activeAmbientTrack = null
                }
                track?.pause()
                track?.flush()
                track?.stop()
                track?.release()
            }
        }
    }

    private fun applyTrackVolume() {
        val vol = (ambientGain * ambientFadeMultiplier).coerceIn(0f, 1f)
        runCatching {
            activeAmbientTrack?.setVolume(vol)
        }
    }

    fun updateAmbientVolume(volume: Float) {
        ambientGain = volume.coerceIn(0f, 1f)
        applyTrackVolume()
    }

    fun setAmbientFadeMultiplier(multiplier: Float) {
        ambientFadeMultiplier = multiplier.coerceIn(0f, 1f)
        applyTrackVolume()
    }

    fun stopAmbient() {
        currentAmbientMode = AmbientSoundMode.OFF
        ambientJob?.cancel()
        ambientJob = null
        val t = activeAmbientTrack
        activeAmbientTrack = null
        runCatching {
            t?.pause()
            t?.flush()
            t?.stop()
            t?.release()
        }
    }

    fun triggerPageLandHaptic() {
        vibrateOneShot(8L, 72)
    }

    /**
     * Subtle double pulse vibration when the Reading Session Timer reaches 00:00.
     */
    fun triggerTimerCompleteHaptic() {
        runCatching {
            val vibrator = resolveVibrator() ?: return
            if (!vibrator.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0L, 95L, 120L, 130L)
                val amplitudes = intArrayOf(0, 120, 0, 150)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 95L, 120L, 130L), -1)
            }
        }
    }

    private fun vibrateOneShot(durationMs: Long, amplitude: Int) {
        runCatching {
            val vibrator = resolveVibrator() ?: return
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            mgr?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
