# ViMusic Android

Native Android music client with YouTube streaming, local file playback, a ten-band equalizer,
and realtime two-way sync with [vimusic.vercel.app](https://vimusic.vercel.app).

Both clients share one Supabase Postgres database. Favourite a song on the phone and it appears
on the website in about a second.

<p>
  <img alt="Platform" src="https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4-7F52FF">
  <img alt="Compose" src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4">
  <img alt="Version" src="https://img.shields.io/badge/version-1.0.2-blue">
</p>

---

## Contents

- [Features](#features)
- [Requirements](#requirements)
- [Getting started](#getting-started)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [How playback works](#how-playback-works)
- [Limitations](#limitations)
- [Roadmap](#roadmap)
- [Releases](#releases)
- [Credits](#credits)

---

## Features

### Playback
- Stream any YouTube track through the embedded YouTube player
- Play local files from a folder you choose
- Background and lock-screen playback with full media-notification controls
- Video as a third pane beside artwork and lyrics, with native YouTube controls
- Hold a volume key to skip tracks, lock screen included
- Shuffle, repeat, audio focus, headset buttons

### Equalizer
Ten ISO-octave bands with 22 presets, plus preamp, bass boost, stereo width, mono, balance,
speed and pitch. Runs on local files only.

### Library
- Offline-first: the UI reads a local Room database and never blocks on the network
- Realtime two-way sync with Postgres; edits merge field-by-field
- Favourites, Most Played, Recently Played, Playlists and Local
- Full playlist management with cover art
- Filter-as-you-type, sort by title or date added
- Database export and import in the web app's SQLite format

### Search
- YouTube search across songs, videos, albums, artists and playlists
- As-you-type suggestions and synced search history
- Paste any YouTube, YouTube Music, `youtu.be` or Shorts link
- Synced lyrics from [LRCLIB](https://lrclib.net), shared with the web app

### Interface
- Colour scheme generated from album art at runtime, with a WCAG AA contrast guard
- Mini player with swipe-to-change-track, expanding to a full player
- Edge-to-edge, predictive back, navigation rail, OLED black mode
- Guest mode: search and play without an account, read-only

---

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or newer |
| JDK | 17 |
| Supabase | A project with the migrations from the web repo applied (`../ViMusic/backend/supabase/migrations/`) |
| Google OAuth | A web client ID and an Android client registration |

---

## Getting started

```bash
git clone https://github.com/ab007shetty/ViMusicAndroid.git
cd ViMusicAndroid
```

Create `local.properties` in the project root:

```properties
sdk.dir=C:\\Users\\<you>\\AppData\\Local\\Android\\Sdk

SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon key>
GOOGLE_WEB_CLIENT_ID=<web client id>.apps.googleusercontent.com
```

Build and install:

```bash
./gradlew installDebug
```

**[SETUP.md](SETUP.md) is the full walkthrough** — database migrations, OAuth registration and
device setup. The app will not sign in or sync without those steps.

---

## Architecture

Multi-module, offline-first, unidirectional data flow.

```
app/                MainActivity, navigation, settings, setup wizard, DI graph
core/model          Domain types and the canonical user_id rule
core/database       Room mirror of the Postgres schema, plus the sync outbox
core/datastore      Preferences
core/innertube      YouTube search, suggestions and metadata
core/data           Supabase, auth, repositories, sync push/pull/realtime
core/media          Playback service, embedded player, local playback, DSP
core/designsystem   Theme, motion, haptics, shared components
feature/library     Tabs, playlists, filtering, sorting
feature/search      Search and link resolution
feature/player      Mini and full player, video, equalizer
```

Reads come from Room, always. Writes go to Room first and reach the network afterwards.

```
Screens ──read──▶ Room  (online or not)
                   │
   write ──────────┼──▶ Room + sync_outbox   (instant, no network)
                   │
        SyncWorker ┴──▶ Supabase ──┬── pull ─────▶ Room
                                   └── realtime ─▶ Room
```

Identity is the join key between the two clients: `user_id` is the Google account email,
lowercased and trimmed. Same account, same rows, no mapping table.

---

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin 2.4 |
| UI | Jetpack Compose, Material 3 |
| Playback | Media3 / ExoPlayer 1.11 |
| Database | Room 2.8 |
| Backend | Supabase (Postgres, Auth, Realtime) 3.8 |
| Networking | Ktor 3.5 |
| DI | Hilt 2.60 |
| Images | Coil 3.6 |

---

## How playback works

Streamed tracks play through YouTube's own IFrame player in a WebView, the same way the web app
does. Local files play through ExoPlayer with the full DSP chain.

The app was first built on ExoPlayer with a direct Innertube stream resolver. Every stream died
after roughly forty seconds, and the cap survived poToken attestation, every Innertube client
and a partial SABR implementation — the throttle is applied to the URL, not to the client
requesting it. Embedding the sanctioned player was the fix.

Two consequences follow from that and cannot be engineered around:

- Streamed audio is never cached, so it has no offline mode
- Streamed audio never enters this process, so the equalizer cannot touch it

The Innertube client remains in the tree for search, suggestions and metadata.

---

## Limitations

| | Why |
|---|---|
| No offline playback of streamed songs | The embedded player owns the audio; there is nothing to cache |
| No equalizer for streamed songs | Same reason — no PCM access |
| No cloud playlist reordering | Playlists keep the order they were built in, matching the website |
| No radio or autoplay | The queue stops when exhausted |
| No picture-in-picture | Fullscreen works through YouTube's own control |
| Not on the Play Store | See below |

**Play Store.** Distribution is by sideload. Background playback of an embedded YouTube player
conflicts with the Embedded Player API terms, and using the Accessibility API for volume-key
skipping is outside Play's accepted uses. A local-files-only build would be publishable, and
would be a different app.

---

## Roadmap

**Next**
- Add a LICENSE file
- Finish or remove the stubbed Android Auto path
- Sleep timer

**Planned**
- Radio and autoplay to extend an exhausted queue
- Home-screen widget and Quick Settings tile
- Crossfade and gapless playback between local tracks

**Blocked**
- Offline streamed songs, pinned downloads and the equalizer for streamed audio. All three need
  PCM access the embedded player does not provide.

---

## Releases

Signed APKs are published on the
[releases page](https://github.com/ab007shetty/ViMusicAndroid/releases). The app checks for
updates itself under **Settings → About → Version**.

Play Protect will warn on install because the APK is not distributed through the Play Store.
Choose **More details → Install anyway**.

To cut a release, see [RELEASING.md](RELEASING.md).

---

## Credits

- [ViMusic](https://github.com/vfsfitvnm/ViMusic) — the original Android app this is named after
- [LRCLIB](https://lrclib.net) — synced lyrics

## License

Not yet licensed. All rights reserved until a LICENSE file is added.
