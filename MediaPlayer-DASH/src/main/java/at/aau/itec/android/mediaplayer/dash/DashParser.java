/*
 * Copyright (c) 2014 Mario Guggenberger <mg@itec.aau.at>
 *
 * This file is part of ITEC MediaPlayer.
 *
 * ITEC MediaPlayer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ITEC MediaPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ITEC MediaPlayer.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.aau.itec.android.mediaplayer.dash;

import android.net.Uri;
import android.util.Log;
import android.util.Xml;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.aau.itec.android.mediaplayer.UriSource;

/**
 * Created by maguggen on 27.08.2014.
 */
public class DashParser {

    private static final String TAG = DashParser.class.getSimpleName();

    private static Pattern PATTERN_TIME = Pattern.compile("PT((\\d+)H)?((\\d+)M)?((\\d+(\\.\\d+)?)S)");
    private static Pattern PATTERN_TEMPLATE = Pattern.compile("\\$(\\w+)(%0\\d+d)?\\$");

    private static class SegmentTemplate {

        private static class SegmentTimelineEntry {
            /**
             * The segment start time in timescale units. Optional value.
             * Default is 0 for the first element in a timeline, and t+d*(r+1) of previous element
             * for all subsequent elements.
             */
            long t;

            /**
             * The segment duration in timescale units.
             */
            long d;

            /**
             * The segment repeat count. Specifies the number of contiguous segments with
             * duration d, that follow the first segment (r=2 means first segment plus two
             * following segments, a total of 3).
             * A negative number tells that there are contiguous segments until the start of the
             * next timeline entry, the end of the period, or the next MPD update.
             * The default is 0.
             */
            int r;

            long calculateDuration() {
                return d * (r + 1);
            }
        }

        long presentationTimeOffset;
        long timescale;
        String init;
        String media;
        long duration;
        int startNumber;
        List<SegmentTimelineEntry> timeline = new ArrayList<>();

        long calculateDurationUs() {
            return calculateUs(duration, timescale);
        }

        boolean hasTimeline() {
            return !timeline.isEmpty();
        }
    }

    /**
     * Parses an MPD XML file. This needs to be executed off the main thread, else a
     * NetworkOnMainThreadException gets thrown.
     * @param source the URl of an MPD XML file
     * @return a MPD object
     * @throws android.os.NetworkOnMainThreadException if executed on the main thread
     */
    public MPD parse(UriSource source) throws DashParserException {
        MPD mpd = null;
        OkHttpClient httpClient = new OkHttpClient();

        Headers.Builder headers = new Headers.Builder();
        if(source.getHeaders() != null && !source.getHeaders().isEmpty()) {
            for(String name : source.getHeaders().keySet()) {
                headers.add(name, source.getHeaders().get(name));
            }
        }

        Uri uri = source.getUri();

        Request.Builder request = new Request.Builder()
                .url(uri.toString())
                .headers(headers.build());

        try {
            Response response = httpClient.newCall(request.build()).execute();
            if(!response.isSuccessful()) {
                throw new IOException("error requesting the MPD");
            }

            // Determine this MPD's default BaseURL by removing the last path segment (which is the MPD file)
            Uri baseUrl = Uri.parse(uri.toString().substring(0, uri.toString().lastIndexOf("/") + 1));

            // Parse the MPD file
            mpd = parse(response.body().byteStream(), baseUrl);
        } catch (IOException e) {
            Log.e(TAG, "error downloading the MPD", e);
            throw new DashParserException("error downloading the MPD", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "error parsing the MPD", e);
            throw new DashParserException("error parsing the MPD", e);
        }

        return mpd;
    }

    private MPD parse(InputStream in, Uri baseUrl) throws XmlPullParserException, IOException, DashParserException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);

            MPD mpd = new MPD();

            int type = 0;
            while((type = parser.next()) >= 0) {
                if(type == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();

                    if(tagName.equals("MPD")) {
                        mpd.mediaPresentationDurationUs = getAttributeValueTime(parser, "mediaPresentationDuration");
                        mpd.minBufferTimeUs = getAttributeValueTime(parser, "minBufferTime");
                    } else if(tagName.equals("BaseURL")) {
                        baseUrl = extendUrl(baseUrl, parser.nextText());
                        Log.d(TAG, "base url: " + baseUrl);
                    } else if(tagName.equals("AdaptationSet")) {
                        mpd.adaptationSets.add(readAdaptationSet(mpd, baseUrl, parser));
                    }
                } else if(type == XmlPullParser.END_TAG) {
                    String tagName = parser.getName();
                    if(tagName.equals("MPD")) {
                        break;
                    }
                }
            }

            Log.d(TAG, mpd.toString());

            return mpd;
        } finally {
            in.close();
        }
    }

    private AdaptationSet readAdaptationSet(MPD mpd, Uri baseUrl, XmlPullParser parser)
            throws XmlPullParserException, IOException, DashParserException {
        AdaptationSet adaptationSet = new AdaptationSet();

        adaptationSet.group = getAttributeValueInt(parser, "group");
        adaptationSet.mimeType = getAttributeValue(parser, "mimeType");

        SegmentTemplate segmentTemplate = null;

        int type = 0;
        while((type = parser.next()) >= 0) {
            if(type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if(tagName.equals("SegmentTemplate")) {
                    segmentTemplate = readSegmentTemplate(parser, baseUrl);
                } else if(tagName.equals("Representation")) {
                    adaptationSet.representations.add(readRepresentation(
                            mpd, adaptationSet, baseUrl, parser, segmentTemplate));
                }
            } else if(type == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if(tagName.equals("AdaptationSet")) {
                    return adaptationSet;
                }
            }
        }

        throw new DashParserException("invalid state");
    }

    private Representation readRepresentation(MPD mpd, AdaptationSet adaptationSet, Uri baseUrl,
                                              XmlPullParser parser, SegmentTemplate segmentTemplate)
            throws XmlPullParserException, IOException, DashParserException {
        Representation representation = new Representation();

        representation.id = getAttributeValue(parser, "id");
        representation.codec = getAttributeValue(parser, "codecs");
        representation.mimeType = getAttributeValue(parser, "mimeType", adaptationSet.mimeType);
        if(representation.mimeType.startsWith("video/")) {
            representation.width = getAttributeValueInt(parser, "width");
            representation.height = getAttributeValueInt(parser, "height");
        }
        representation.bandwidth = getAttributeValueInt(parser, "bandwidth");

        int type = 0;
        while((type = parser.next()) >= 0) {
            String tagName = parser.getName();

            if(type == XmlPullParser.START_TAG) {
                if (tagName.equals("Initialization")) {
                    representation.initSegment = new Segment(
                            extendUrl(baseUrl, getAttributeValue(parser, "sourceURL")).toString(),
                            getAttributeValue(parser, "range"));
                    Log.d(TAG, "Initialization: " + representation.initSegment.toString());
                } else if(tagName.equals("SegmentList")) {
                    long timescale = getAttributeValueLong(parser, "timescale", 1);
                    long duration = getAttributeValueLong(parser, "duration");
                    representation.segmentDurationUs = (long)(((double)duration / timescale) * 1000000d);
                } else if(tagName.equals("SegmentURL")) {
                    representation.segments.add(new Segment(
                            extendUrl(baseUrl, getAttributeValue(parser, "media")).toString(),
                            getAttributeValue(parser, "mediaRange")));
                } else if(tagName.equals("SegmentTemplate")) {
                    // Overwrite passed template with newly parsed one
                    segmentTemplate = readSegmentTemplate(parser, baseUrl);
                } else if(tagName.equals("BaseURL")) {
                    baseUrl = extendUrl(baseUrl, parser.nextText());
                    Log.d(TAG, "new base url: " + baseUrl);
                }
            } else if(type == XmlPullParser.END_TAG) {
                if(tagName.equals("Representation")) {

                    // If there is a segment template, expand it to a list of segments
                    if(segmentTemplate != null) {
                        if(segmentTemplate.hasTimeline()) {
                            if(segmentTemplate.timeline.size() > 1) {
                                /* TODO Add support for individual segment lengths
                                 * To support multiple timeline entries, the segmentDurationUs
                                 * must be moved from the representation to the individual segments,
                                 * because their length is not necessarily constant and can change
                                 * over time.
                                 */
                                throw new DashParserException("timeline with multiple entries is not supported yet");
                            }

                            SegmentTemplate.SegmentTimelineEntry current, previous, next;
                            for(int i = 0; i < segmentTemplate.timeline.size(); i++) {
                                current = segmentTemplate.timeline.get(i);
                                //previous = i > 0 ? segmentTemplate.timeline.get(i - 1) : null;
                                next = i < segmentTemplate.timeline.size() - 1 ? segmentTemplate.timeline.get(i + 1) : null;

                                int repeat = current.r;
                                if(repeat < 0) {
                                    long duration = next != null ? next.t - current.t :
                                            calculateTimescaleTime(mpd.mediaPresentationDurationUs, segmentTemplate.timescale) - current.t;
                                    repeat = (int)(duration / current.d) - 1;
                                }

                                representation.segmentDurationUs = calculateUs(current.d, segmentTemplate.timescale);

                                // init segment
                                String processedInitUrl = processMediaUrl(
                                        segmentTemplate.init, representation.id, null, representation.bandwidth, null);
                                representation.initSegment = new Segment(processedInitUrl);

                                // media segments
                                long time = current.t;
                                for (int j = segmentTemplate.startNumber; j < repeat + 1; j++) {
                                    String processedMediaUrl = processMediaUrl(
                                            segmentTemplate.media, representation.id, null, representation.bandwidth, time);
                                    representation.segments.add(new Segment(processedMediaUrl));
                                    time += current.d;
                                }
                            }
                        }
                        else {
                            representation.segmentDurationUs = segmentTemplate.calculateDurationUs();
                            int numSegments = (int) Math.ceil((double) mpd.mediaPresentationDurationUs / representation.segmentDurationUs);

                            // init segment
                            String processedInitUrl = processMediaUrl(
                                    segmentTemplate.init, representation.id, null, representation.bandwidth, null);
                            representation.initSegment = new Segment(processedInitUrl);

                            // media segments
                            for (int i = segmentTemplate.startNumber; i < segmentTemplate.startNumber + numSegments; i++) {
                                String processedMediaUrl = processMediaUrl(
                                        segmentTemplate.media, representation.id, i, representation.bandwidth, null);
                                representation.segments.add(new Segment(processedMediaUrl));
                            }
                        }
                    }

                    Log.d(TAG, representation.toString());

                    return representation;
                }
            }
        }

        throw new DashParserException("invalid state");
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private SegmentTemplate readSegmentTemplate(XmlPullParser parser, Uri baseUrl)
            throws IOException, XmlPullParserException, DashParserException {
        SegmentTemplate st = new SegmentTemplate();

        st.presentationTimeOffset = getAttributeValueLong(parser, "presentationTimeOffset"); // TODO use this?
        st.timescale = getAttributeValueLong(parser, "timescale", 1);
        st.duration = getAttributeValueLong(parser, "duration");
        st.startNumber = getAttributeValueInt(parser, "startNumber");
        st.init = extendUrl(baseUrl, getAttributeValue(parser, "initialization")).toString();
        st.media = extendUrl(baseUrl, getAttributeValue(parser, "media")).toString();

        int type = 0;
        while((type = parser.next()) >= 0) {
            if(type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if(tagName.equals("S")) {
                    SegmentTemplate.SegmentTimelineEntry e = new SegmentTemplate.SegmentTimelineEntry();

                    long defaultTime = 0;
                    if(!st.timeline.isEmpty()) {
                        SegmentTemplate.SegmentTimelineEntry previous = st.timeline.get(st.timeline.size() - 1);
                        defaultTime = previous.t + previous.calculateDuration();
                    }

                    e.t = getAttributeValueLong(parser, "t", defaultTime);
                    e.d = getAttributeValueLong(parser, "d");
                    e.r = getAttributeValueInt(parser, "r");

                    st.timeline.add(e);
                }
            } else if(type == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if(tagName.equals("SegmentTimeline")) {
                    return st;
                }
            }
        }

        throw new DashParserException("invalid state");
    }

    /**
     * Parse a timestamp and return its duration in microseconds.
     * http://en.wikipedia.org/wiki/ISO_8601#Durations
     */
    private static long parseTime(String time) {
        Matcher matcher = PATTERN_TIME.matcher(time);

        if(matcher.matches()) {
            long hours = 0;
            long minutes = 0;
            double seconds = 0;

            String group = matcher.group(2);
            if (group != null) {
                hours = Long.parseLong(group);
            }
            group = matcher.group(4);
            if (group != null) {
                minutes = Long.parseLong(group);
            }
            group = matcher.group(6);
            if (group != null) {
                seconds = Double.parseDouble(group);
            }

            return (long) (seconds * 1000 * 1000)
                    + minutes * 60 * 1000 * 1000
                    + hours * 60 * 60 * 1000 * 1000;
        }

        return -1;
    }

    /**
     * Extends an URL with an extended path if the extension is relative, or replaces the entire URL
     * with the extension if it is absolute.
     */
    private static Uri extendUrl(Uri url, String urlExtension) {
        Uri newUrl = Uri.parse(urlExtension);

        if(newUrl.isRelative()) {
            newUrl = Uri.withAppendedPath(url, urlExtension);
        }
        return newUrl;
    }

    /**
     * Converts a time/timescale pair to microseconds.
     */
    private static long calculateUs(long time, long timescale) {
        return (long)(((double)time / timescale) * 1000000d);
    }

    private static long calculateTimescaleTime(long time, long timescale) {
        return (long)((time / 1000000d) * timescale);
    }

    private static String getAttributeValue(XmlPullParser parser, String name, String defValue) {
        String value = parser.getAttributeValue(null, name);
        return value != null ? value : defValue;
    }

    private static String getAttributeValue(XmlPullParser parser, String name) {
        return getAttributeValue(parser, name, null);
    }

    private static int getAttributeValueInt(XmlPullParser parser, String name) {
        return Integer.parseInt(getAttributeValue(parser, name, "0"));
    }

    private static long getAttributeValueLong(XmlPullParser parser, String name) {
        return Long.parseLong(getAttributeValue(parser, name, "0"));
    }

    private static long getAttributeValueLong(XmlPullParser parser, String name, long defValue) {
        return Long.parseLong(getAttributeValue(parser, name, defValue+""));
    }

    private static long getAttributeValueTime(XmlPullParser parser, String name) {
        return parseTime(getAttributeValue(parser, name));
    }

    /**
     * Processes templates in media URLs.
     * 
     * Example: $RepresentationID$_$Number%05d$.ts
     *
     * 5.3.9.4.4 Template-based Segment URL construction
     * Table 16 - Identifiers for URL templates
     */
    private static String processMediaUrl(String url, String representationId,
                                          Integer number, Integer bandwidth, Long time) {
        // $$
        url = url.replace("$$", "$");

        // RepresentationID
        if(representationId != null) {
            url = url.replace("$RepresentationID$", representationId);
        }

        // Number, Bandwidth & Time with formatting support
        // The following block converts DASH segment URL templates to a Java String.format expression

        List<String> templates = Arrays.asList("Number", "Bandwidth", "Time");
        Matcher matcher = PATTERN_TEMPLATE.matcher(url);

        while(matcher.find()) {
            String template = matcher.group(1);
            String pattern = matcher.group(2);
            int index = templates.indexOf(template);

            if(pattern != null) {
                url = url.replace("$" + template + pattern + "$",
                        "%" + (index + 1) + "$" + pattern.substring(1));
            } else {
                // Table 16: If no format tag is present, a default format tag with width=1 shall be used.
                url = url.replace("$" + template + "$", "%" + (index + 1) + "$01d");
            }
        }

        url = String.format(url, number, bandwidth, time); // order must match templates list above

        return url;
    }
}
