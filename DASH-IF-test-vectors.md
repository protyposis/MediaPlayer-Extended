DASH-IF Test Vector evaluation
==============================

This table is an evaluation of the compatibility of MediaPlayer-Extended with the DASH test vectors
available here: http://dashif.org/test-vectors/

Tested on a Nexus 4 with Android 5.1.1.

Standard Definition MPDs
------------------------
### Single Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation
Test Vector 4  | nope, single-segment representation
Test Vector 5  | nope, number overflow in media header box, isoparser does not support UInt64
Test Vector 6  | nope, unsupported video codec
Test Vector 7  | CHECK
Test Vector 8  | CHECK
Test Vector 9  | CHECK
Test Vector 10 | nope, timeline with multiple entries
Test Vector 11 | CHECK
Test Vector 12 | CHECK

### Multi-Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, unsupported video codec
Test Vector 4  | CHECK
Test Vector 5  | CHECK
Test Vector 6  | CHECK
Test Vector 7  | CHECK
Test Vector 8  | CHECK

### Multiple Audio Representations

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation
Test Vector 4  | nope, single-segment representation
Test Vector 5  | nope, single-segment representation
Test Vector 6  | nope, single-segment representation
Test Vector 8  | nope, single-segment representation
Test Vector 9  | nope, single-segment representation
Test Vector 10 | nope, single-segment representation
Test Vector 11 | nope, single-segment representation
Test Vector 12 | nope, single-segment representation
Test Vector 13 | nope, single-segment representation
Test Vector 14 | nope, single-segment representation

### Addition of Subtitles

Subtitle are not supported yet.

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation
Test Vector 4  | nope, single-segment representation

### Multiple Periods

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | CHECK, only first period
Test Vector 3  | CHECK, only first period
Test Vector 4  | CHECK, only first period
Test Vector 5  | CHECK, only first period
Test Vector 11 | CHECK, only first period
Test Vector 12 | CHECK, only first period
Test Vector 13 | CHECK, only first period
Test Vector 14 | CHECK, only first period
Test Vector 15 | CHECK, only first period
Test Vector 16 | CHECK, only first period
Test Vector 17 | CHECK, only first period

### Encryption and Key Rotations

Encryption is not supported yet.

### Dynamic Segment Offering

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | not really, request timing (segment number) problems
Test Vector 2  | not really, request timing (segment number) problems
Test Vector 3  | not really, request timing (segment number) problems

### Dynamic Segment Offering with MPD Update

MPD update is not supported yet.

Test Vector    | Result
-------------- | ---------------
Test Vector 2  | timeout, no answer from server
Test Vector 3  | CHECK, experimental playback
Test Vector 4  | CHECK, experimental playback
Test Vector 5  | CHECK, experimental playback
Test Vector 6  | CHECK, experimental playback
Test Vector 7  | CHECK, experimental playback
Test Vector 8  | CHECK, experimental playback
Test Vector 9  | CHECK, experimental playback

### Addition of Trick Mode

Trick modes are not supported.

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | CHECK
Test Vector 4  | CHECK
Test Vector 5  | CHECK

High Definition MPDs
--------------------
### Single Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | CHECK
Test Vector 4  | CHECK
Test Vector 5  | CHECK

### Multi-Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation
Test Vector 4  | CHECK
Test Vector 5  | CHECK
Test Vector 6  | CHECK
Test Vector 7  | CHECK

Multichannel Audio Extensions
-----------------------------

### Dolby

#### 6-Channel ID

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### 8-Channel ID

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### Single Stereo Audio Track

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### Multiple Adaptation Sets

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### AC-4 Test Vectors

AC-4 is not supported by Android 5.1.1

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation
Test Vector 4  | nope, single-segment representation
Test Vector 5  | nope, single-segment representation
Test Vector 6  | nope, single-segment representation
Test Vector 7  | audio codec not supported, picture playback only
Test Vector 8  | audio codec not supported, picture playback only
Test Vector 9  | audio codec not supported, picture playback only
Test Vector 10 | audio codec not supported, picture playback only
Test Vector 11 | audio codec not supported, picture playback only
Test Vector 12 | audio codec not supported, picture playback only

### DTS

#### Single Multichannel Audio Track

Segment server is case sensitive and segment URLs are specified with wrong casing in the MPD.

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | segments 404
Test Vector 2  | segments 404
Test Vector 3  | segments 404
Test Vector 4  | segments 404

#### Single Stereo Audio Track

Segment server is case sensitive and segment URLs are specified with wrong casing in the MPD.

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | segments 404
Test Vector 2  | segments 404
Test Vector 3  | segments 404

#### Multiple Adaptation Sets

Segment server is case sensitive and segment URLs are specified with wrong casing in the MPD.

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | segments 404
Test Vector 2  | segments 404
Test Vector 3  | segments 404
Test Vector 4  | segments 404

### HE-AACv2 Multichannel

#### 6-Channel ID
Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### 8-Channel ID

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### Multiple Audio Representations

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | nope, single-segment representation

### MPEG Surround

#### 6-Channel ID

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation

#### Multiple Audio Representations

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation

HEVC Test Vectors
-----------------

### Single Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | CHECK
Test Vector 4  | CHECK

### Multi-Resolution Multi-Rate

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation
Test Vector 3  | CHECK
Test Vector 4  | CHECK

Negative Test Vectors
---------------------

### Essential Property

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation

### Content Protection

Test Vector    | Result
-------------- | ---------------
Test Vector 1  | nope, single-segment representation
Test Vector 2  | nope, single-segment representation