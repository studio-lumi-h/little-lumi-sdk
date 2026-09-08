package com.group_finity.mascot.lumi.plugin;

import javax.sound.sampled.AudioFormat;

/**
 * 재생할 소리 한 토막 — {@link PluginContext#speak}. TTS 응답이든 미리 구운 파일이든 <b>PCM</b> 으로
 * 풀어서 건넨다({@code javax.sound.sampled.AudioSystem} 이 mp3 는 못 읽는다; wav 는
 * {@code AudioSystem.getAudioInputStream} 으로 바로 PCM 이다).
 *
 * @param format 샘플 형식 (예: 16-bit signed little-endian, 44100 Hz, mono)
 * @param pcm    샘플 바이트. 복사하지 않는다 — 큰 배열이라 건넨 뒤 만지지 말 것
 * @param millis 길이(밀리초). 말풍선을 소리 길이에 맞출 때 쓴다; 모르면 0
 */
public record PcmAudio(AudioFormat format, byte[] pcm, long millis) {

    public PcmAudio {
        if (format == null) {
            throw new IllegalArgumentException("format is null");
        }
        if (pcm == null) {
            pcm = new byte[0];
        }
        if (millis < 0) {
            millis = 0;
        }
    }

    /** 길이를 모를 때 형식과 바이트 수로 셈한다. */
    public static long millisOf(AudioFormat format, int bytes) {
        if (format == null || format.getFrameSize() <= 0 || format.getFrameRate() <= 0) {
            return 0;
        }
        return (long) (bytes / (double) format.getFrameSize() / format.getFrameRate() * 1000.0);
    }
}
