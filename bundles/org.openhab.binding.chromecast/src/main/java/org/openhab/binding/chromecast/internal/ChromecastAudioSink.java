/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.chromecast.internal;

import java.util.Objects;

import org.eclipse.smarthome.core.audio.AudioFormat;
import org.eclipse.smarthome.core.audio.AudioHTTPServer;
import org.eclipse.smarthome.core.audio.AudioStream;
import org.eclipse.smarthome.core.audio.FixedLengthAudioStream;
import org.eclipse.smarthome.core.audio.URLAudioStream;
import org.eclipse.smarthome.core.audio.UnsupportedAudioFormatException;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the AudioSink portion of the Chromecast plugin. Note that we store volume in
 *
 * @author Jason Holmes - Initial contribution
 */
public class ChromecastAudioSink {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChromecastCommander commander;
    private final AudioHTTPServer audioHTTPServer;
    private final String callbackUrl;

    public ChromecastAudioSink(ChromecastCommander commander, AudioHTTPServer audioHTTPServer, String callbackUrl) {
        this.commander = commander;
        this.audioHTTPServer = audioHTTPServer;
        this.callbackUrl = callbackUrl;
    }

    public void process(AudioStream audioStream) throws UnsupportedAudioFormatException {
        if (audioStream == null) {
            // in case the audioStream is null, this should be interpreted as a request to end any currently playing
            // stream.
            logger.trace("Stop currently playing stream.");
            commander.handleStop(OnOffType.ON);
        } else {
            String url;
            if (audioStream instanceof URLAudioStream) {
                // it is an external URL, the speaker can access it itself and play it.
                URLAudioStream urlAudioStream = (URLAudioStream) audioStream;
                url = urlAudioStream.getURL();
            } else {
                if (callbackUrl != null) {
                    // we serve it on our own HTTP server
                    String relativeUrl;
                    if (audioStream instanceof FixedLengthAudioStream) {
                        relativeUrl = audioHTTPServer.serve((FixedLengthAudioStream) audioStream, 10);
                    } else {
                        relativeUrl = audioHTTPServer.serve(audioStream);
                    }
                    url = callbackUrl + relativeUrl;
                } else {
                    logger.warn("We do not have any callback url, so Chromecast cannot play the audio stream!");
                    return;
                }
            }

            String mimeType = Objects.equals(audioStream.getFormat().getCodec(), AudioFormat.CODEC_MP3) ? "audio/mpeg"
                    : "audio/wav";

            commander.playMedia("Notification", url, mimeType);
        }
    }
}
