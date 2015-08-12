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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
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
    private static DateFormat ISO8601UTC;

    static {
        ISO8601UTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        ISO8601UTC.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

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
                        mpd.isDynamic = getAttributeValue(parser, "type", "static").equals("dynamic");

                        if (mpd.isDynamic) {
                            Log.i(TAG, "dynamic MPD not supported yet, but giving it a try...");
                            // Set a dummy duration to get the stream to work for some time
                            mpd.mediaPresentationDurationUs = 1l /* h */ * 60 * 60 * 1000000;
                            mpd.timeShiftBufferDepthUs = getAttributeValueTime(parser, "timeShiftBufferDepth", "PT0S");
                            mpd.maxSegmentDurationUs = getAttributeValueTime(parser, "maxSegmentDuration", "PT0S");
                            mpd.suggestedPresentationDelayUs = getAttributeValueTime(parser, "suggestedPresentationDelay", "PT0S");
                            // TODO add support for dynamic streams with unknown duration

                            String date = getAttributeValue(parser, "availabilityStartTime");
                            try {
                                if(date.length() == 19) {
                                    date = date + "Z";
                                }
                                mpd.availabilityStartTime = ISO8601UTC.parse(date.replace("Z", "+00:00"));
                            } catch (ParseException e) {
                                Log.e(TAG, "unable to parse date: " + date);
                            }
                        }
                         else { // type == static
                             mpd.mediaPresentationDurationUs = getAttributeValueTime(parser, "mediaPresentationDuration");
                         }
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
        adaptationSet.maxWidth = getAttributeValueInt(parser, "maxWidth");
        adaptationSet.maxHeight = getAttributeValueInt(parser, "maxHeight");
        adaptationSet.par = getAttributeValueRatio(parser, "par");

        SegmentTemplate segmentTemplate = null;

        int type = 0;
        while((type = parser.next()) >= 0) {
            if(type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                if(tagName.equals("SegmentTemplate")) {
                    segmentTemplate = readSegmentTemplate(parser, baseUrl, null);
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
            representation.sar = getAttributeValueRatio(parser, "sar");
        }
        representation.bandwidth = getAttributeValueInt(parser, "bandwidth");

        int type = 0;
        while((type = parser.next()) >= 0) {
            String tagName = parser.getName();

            if(type == XmlPullParser.START_TAG) {
                if (tagName.equals("Initialization")) {
                    String sourceURL = getAttributeValue(parser, "sourceURL");
                    String range = getAttributeValue(parser, "range");

                    sourceURL = sourceURL != null ? extendUrl(baseUrl, sourceURL).toString() : baseUrl.toString();

                    representation.initSegment = new Segment(sourceURL, range);
                    Log.d(TAG, "Initialization: " + representation.initSegment.toString());
                } else if(tagName.equals("SegmentList")) {
                    long timescale = getAttributeValueLong(parser, "timescale", 1);
                    long duration = getAttributeValueLong(parser, "duration");
                    representation.segmentDurationUs = (long)(((double)duration / timescale) * 1000000d);
                } else if(tagName.equals("SegmentURL")) {
                    String media = getAttributeValue(parser, "media");
                    String mediaRange = getAttributeValue(parser, "mediaRange");
                    String indexRange = getAttributeValue(parser, "indexRange");

                    media = media != null ? extendUrl(baseUrl, media).toString() : baseUrl.toString();

                    representation.segments.add(new Segment(media, mediaRange));

                    if(indexRange != null) {
                        Log.v(TAG, "skipping unsupported indexRange in SegmentURL");
                    }
                } else if(tagName.equals("SegmentBase")) {
                    String indexRange = getAttributeValue(parser, "indexRange");
                    if(indexRange != null) {
                        throw new DashParserException("single segment / indexRange is not supported yet");
                    }
                } else if(tagName.equals("SegmentTemplate")) {
                    // Overwrite passed template with newly parsed one
                    segmentTemplate = readSegmentTemplate(parser, baseUrl, segmentTemplate);
                } else if(tagName.equals("BaseURL")) {
                    baseUrl = extendUrl(baseUrl, parser.nextText());
                    Log.d(TAG, "new base url: " + baseUrl);
                } else if(tagName.equals("RepresentationIndex")) {
                    throw new DashParserException("RepresentationIndex is not supported yet");
                }
            } else if(type == XmlPullParser.END_TAG) {
                if(tagName.equals("Representation")) {
                    if(!representation.segments.isEmpty()) {
                        // a SegmentList has been parsed, nothing to do here
                    }
                    else if(segmentTemplate != null) {
                        // We have a SegmentTemplate, expand it to a list of segments

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

                            if(mpd.isDynamic) {
                                // Simulate availabilityStartTime support by converting it to a startNumber
                                Date now = new Date();
                                Calendar calendar = Calendar.getInstance();

                                calendar.setTime(now);
                                calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
                                now = calendar.getTime();

                                /* Calculate the time delta between the availability start time
                                 * and the current time, for that we know at which position we
                                 * currently are in the live stream. */
                                // TODO sync local time with server time (from http date header)?
                                long availabilityDeltaTimeUs = (now.getTime() - mpd.availabilityStartTime.getTime()) * 1000;

                                // go back in time by the buffering period (else the segments to be buffered are not available yet)
                                availabilityDeltaTimeUs -= Math.max(mpd.minBufferTimeUs, 10 * 1000000L);

                                // go back in time by the suggested presentation delay
                                availabilityDeltaTimeUs -= mpd.suggestedPresentationDelayUs;

                                // convert the delta time to the number of corresponding segments
                                // add it to the start number (which by default is 0 if not specified)
                                segmentTemplate.startNumber += (int)(availabilityDeltaTimeUs / representation.segmentDurationUs);
                            }

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
                    else {
                        /* When there is no SegmentList or SegmentTemplate, the only option left is
                         * a single file/segment representation. */

                        // Subtitle are not supported yet and can be ignored
                        if(representation.mimeType != null && representation.mimeType.startsWith("text/")) {
                            Log.i(TAG, "unsupported subtitle representation");
                        }
                        // Video and audio representations are vital for the player and cannot be ignored
                        else {
                            throw new DashParserException("single-segment representations are not supported yet");
                            // TODO implement single-file/single-segment support
                            // TODO add SegmentBase and sidx downloading and parsing
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

    private SegmentTemplate readSegmentTemplate(XmlPullParser parser, Uri baseUrl, SegmentTemplate parent)
            throws IOException, XmlPullParserException, DashParserException {
        SegmentTemplate st = new SegmentTemplate();

        // Read properties from template or carry them over from a parent

        st.presentationTimeOffset = getAttributeValueLong(parser, "presentationTimeOffset", parent != null ? parent.presentationTimeOffset : 0); // TODO use this?
        st.timescale = getAttributeValueLong(parser, "timescale", parent != null ? parent.timescale : 1);
        st.duration = getAttributeValueLong(parser, "duration", parent != null ? parent.duration : 0);
        st.startNumber = getAttributeValueInt(parser, "startNumber", parent != null ? parent.startNumber : 0);

        String initialization = getAttributeValue(parser, "initialization");
        if(initialization != null) {
            st.init = extendUrl(baseUrl, initialization).toString();
        } else if(parent != null) {
            st.init = parent.init;
        }

        String media = getAttributeValue(parser, "media");
        if(media != null) {
            st.media = extendUrl(baseUrl, media).toString();
        } else if(parent != null) {
            st.media = parent.media;
        }

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
                } else if(tagName.equals("RepresentationIndex")) {
                    throw new DashParserException("RepresentationIndex is not supported yet");
                }
            } else if(type == XmlPullParser.END_TAG) {
                String tagName = parser.getName();
                if(tagName.equals("SegmentTemplate")) {
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
        urlExtension = urlExtension.replace(" ", "%20"); // Convert spaces

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

    private static int getAttributeValueInt(XmlPullParser parser, String name, int defValue) {
        return Integer.parseInt(getAttributeValue(parser, name, defValue+""));
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

    private static long getAttributeValueTime(XmlPullParser parser, String name, String defValue) {
        return parseTime(getAttributeValue(parser, name, defValue));
    }

    private static float getAttributeValueRatio(XmlPullParser parser, String name) {
        String value = getAttributeValue(parser, name);

        if(value != null) {
            String[] values = value.split(":");
            return (float)Integer.parseInt(values[0]) / Integer.parseInt(values[1]);
        }

        return 0;
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

        // $$
        // Replace this at the end, else it breaks directly consecutive template expressions,
        // e.g. $Bandwidth$$Number$.
        url = url.replace("$$", "$");

        return url;
    }
}
