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
import kotlin.random.Random

/**
 * 100% procedural audio & micro-haptics synthesizer (0 KB media assets).
 * - Synthesizes soft, velocity-sensitive paper-rustle audio on AudioTrack.
 * - Synthesizes offline Cozy Ambient Sounds (Soft Rain & Warm Vinyl Crackle) on a streaming AudioTrack.
 */
class PaperAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Streaming cozy ambient synthesizer state
    private var ambientJob: Job? = null
    @Volatile
    private var ambientGain: Float = 0.45f
    @Volatile
    private var ambientFadeMultiplier: Float = 1.0f
    @Volatile
    private var currentAmbientMode: AmbientSoundMode = AmbientSoundMode.OFF

    fun playPageRustle(velocityNormalized: Float = 0.7f, stiffness: Float = 0.55f) {
        val vel = velocityNormalized.coerceIn(0.35f, 1.45f)
        val stiff = stiffness.coerceIn(0.2f, 1.0f)
        scope.launch {
            runCatching {
                val sampleRate = 22050
                val durationSec = (0.26f - (vel - 0.35f) * 0.06f).coerceIn(0.15f, 0.28f)
                val sampleCount = (sampleRate * durationSec).toInt()
                val pcm = ShortArray(sampleCount)

                val hpCutoff = 850f + stiff * 550f + vel * 220f
                val lpCutoff = 2600f + stiff * 1100f + vel * 450f
                val dt = 1f / sampleRate
                val rcHp = 1f / (2f * PI.toFloat() * hpCutoff)
                val alphaHp = rcHp / (rcHp + dt)
                val rcLp = 1f / (2f * PI.toFloat() * lpCutoff)
                val alphaLp = dt / (rcLp + dt)

                val rng = Random(System.nanoTime())
                var prevRaw = 0f
                var prevHp = 0f
                var prevLp = 0f

                val masterGain = (0.14f + vel * 0.09f).coerceAtMost(0.26f)
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
                )
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

                track.write(pcm, 0, sampleCount)
                track.play()
                Thread.sleep((durationSec * 1000f).toLong() + 45L)
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
            return
        }
        stopAmbient()
        currentAmbientMode = mode

        ambientJob = scope.launch {
            val sampleRate = 22050
            val chunkSamples = 2048
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            var track: AudioTrack? = null
            runCatching {
                track = AudioTrack.Builder()
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
                    .setBufferSizeInBytes(maxOf(minBuf, chunkSamples * 4))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                track?.play()

                val pcm = ShortArray(chunkSamples)
                val rng = Random(System.nanoTime())

                // Filter states for organic Soft Rain & Warm Vinyl Crackle
                var brown = 0f
                var lp1 = 0f
                var lp2 = 0f
                var popDecay = 0f
                var phase = 0.0

                while (isActive && currentAmbientMode == mode) {
                    val effectiveGain = (ambientGain * ambientFadeMultiplier).coerceIn(0f, 1f)
                    if (mode == AmbientSoundMode.RAIN) {
                        for (i in 0 until chunkSamples) {
                            val white = rng.nextFloat() * 2f - 1f
                            // Soft pink/brown rain bed
                            brown = (brown + 0.045f * white).coerceIn(-1f, 1f)
                            lp1 += 0.14f * (white - lp1)
                            // Gentle distant rainfall modulation + occasional soft roof droplet
                            val swell = 0.85f + 0.15f * sin(phase).toFloat()
                            phase += 0.00018
                            var drop = 0f
                            if (rng.nextInt(900) == 0) {
                                popDecay = 0.42f + rng.nextFloat() * 0.25f
                            }
                            if (popDecay > 0.001f) {
                                drop = white * popDecay
                                popDecay *= 0.94f
                            }
                            val mix = (brown * 0.42f + lp1 * 0.35f + drop * 0.25f) * swell * 0.38f * effectiveGain
                            pcm[i] = (mix * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    } else {
                        // Warm Vinyl Crackle: cozy low-pass groove hiss + gentle micro-pops + faint 60Hz warmth
                        for (i in 0 until chunkSamples) {
                            val white = rng.nextFloat() * 2f - 1f
                            lp1 += 0.09f * (white - lp1)
                            lp2 += 0.32f * (white - lp2)
                            val warmHum = 0.025f * sin(phase).toFloat()
                            phase += (2.0 * PI * 58.0) / sampleRate

                            var crackle = 0f
                            val roll = rng.nextInt(2600)
                            if (roll == 0) {
                                popDecay = 0.72f + rng.nextFloat() * 0.28f
                            } else if (roll < 5) {
                                crackle = (rng.nextFloat() * 2f - 1f) * 0.24f
                            }
                            if (popDecay > 0.005f) {
                                crackle += (rng.nextFloat() - 0.35f) * popDecay
                                popDecay *= 0.82f
                            }
                            val mix = (lp1 * 0.16f + (lp2 - lp1) * 0.10f + warmHum + crackle * 0.48f) * 0.42f * effectiveGain
                            pcm[i] = (mix * 32767f).toInt().coerceIn(-32767, 32767).toShort()
                        }
                    }
                    track?.write(pcm, 0, chunkSamples)
                }
            }
            runCatching {
                track?.stop()
                track?.release()
            }
        }
    }

    fun updateAmbientVolume(volume: Float) {
        ambientGain = volume.coerceIn(0f, 1f)
    }

    fun setAmbientFadeMultiplier(multiplier: Float) {
        ambientFadeMultiplier = multiplier.coerceIn(0f, 1f)
    }

    fun stopAmbient() {
        currentAmbientMode = AmbientSoundMode.OFF
        ambientJob?.cancel()
        ambientJob = null
    }

    fun triggerPageLandHaptic() {
        runCatching {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(8L, 72))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(8L)
                }
            }
        }
    }
}
