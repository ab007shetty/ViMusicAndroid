# tools

## `backup-supabase.mjs` — full data dump from Supabase

Exports every ViMusic table out of Postgres to JSON plus a replayable
`restore.sql`. Run it before any migration, or whenever you want a snapshot.

### Run it

```bash
cd ViMusicAndroid
node tools/backup-supabase.mjs
```

Somewhere else:

```bash
node tools/backup-supabase.mjs --out D:/backups
```

Needs only Node. No `pg_dump`, no `psql`, no Supabase CLI, no Docker.

### Where it puts things

```
backups/supabase-<timestamp>/
├── users.json
├── song.json
├── playlist.json
├── song_playlist_map.json
├── artist.json  album.json  song_artist_map.json  song_album_map.json
├── format.json  lyrics.json  event.json
└── restore.sql        one INSERT block per table
```

`backups/` is gitignored. These files hold real user data — emails, profiles,
listening history — so they should never be committed or shared.

### How to restore

Paste `restore.sql` into the Supabase SQL editor.

It is **data only** and assumes the schema already exists. Every statement
ends in `ON CONFLICT DO NOTHING`, so restoring into a database that still has
rows tops it up rather than failing on the first duplicate key. That also
means it will not *overwrite* a row that already exists — to replace rather
than merge, delete the rows first.

### Where the credentials come from

It reads `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` from
`../ViMusic/backend/.env`, so no key is stored in this repo.

It refuses to run with the anon key. Row-level security would filter the
results and hand you a backup that looks complete and silently is not — the
worst possible failure mode for a backup.

### What it does to the database

Nothing. It issues `SELECT`s over the REST API and writes only to local
files.

### Last verified

2026-09-06 — 12,297 rows across 11 tables. `playback_state` is skipped
automatically until migration 011 creates it.
