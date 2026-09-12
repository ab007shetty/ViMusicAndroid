#!/usr/bin/env node
/**
 * Dumps every ViMusic table out of Supabase to timestamped JSON, plus a
 * replayable .sql file.
 *
 * Exists because pg_dump is not installed here and the Supabase CLI is not
 * either. This goes through the REST API with the service-role key, so it
 * needs nothing beyond Node and the key already in the web app's .env.
 *
 * Read-only. It issues SELECTs and nothing else.
 *
 * Usage, from the ViMusicAndroid directory:
 *   node tools/backup-supabase.mjs
 *   node tools/backup-supabase.mjs --out D:/backups
 */

import { readFileSync, mkdirSync, writeFileSync, existsSync } from 'node:fs';
import { join, resolve } from 'node:path';

// Tables that actually hold user data. Ordered so the .sql file restores
// cleanly: playlist before song_playlist_map, which references it.
const TABLES = [
  'users',
  'song',
  'playlist',
  'song_playlist_map',
  'artist',
  'album',
  'song_artist_map',
  'song_album_map',
  'format',
  'lyrics',
  'event',
  'playback_state',
];

const PAGE = 1000;

function loadEnv() {
  // The service-role key lives in the web app's backend .env; this script
  // deliberately does not keep its own copy of a credential.
  const candidates = [
    resolve('../ViMusic/backend/.env'),
    resolve('../ViMusic/frontend/.env'),
  ];

  const env = {};
  for (const path of candidates) {
    if (!existsSync(path)) continue;
    for (const line of readFileSync(path, 'utf8').split('\n')) {
      const match = line.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
      if (match) env[match[1]] = match[2].trim();
    }
  }

  const url = env.SUPABASE_URL || env.VITE_SUPABASE_URL;
  // The service role sees every row; the anon key would silently return only
  // what RLS allows, which would make the backup quietly incomplete.
  const key = env.SUPABASE_SERVICE_ROLE_KEY;

  if (!url) throw new Error('No SUPABASE_URL found in ../ViMusic/*/.env');
  if (!key) {
    throw new Error(
      'No SUPABASE_SERVICE_ROLE_KEY in ../ViMusic/backend/.env.\n' +
      'The anon key cannot be used: RLS would filter the dump and you would\n' +
      'get a backup that looks fine and is missing rows.'
    );
  }
  return { url: url.replace(/\/$/, ''), key };
}

async function fetchTable(url, key, table) {
  const rows = [];
  for (let offset = 0; ; offset += PAGE) {
    const response = await fetch(
      `${url}/rest/v1/${table}?select=*&limit=${PAGE}&offset=${offset}`,
      { headers: { apikey: key, Authorization: `Bearer ${key}` } }
    );

    if (response.status === 404) return null; // table does not exist
    if (!response.ok) {
      throw new Error(`${table}: HTTP ${response.status} ${await response.text()}`);
    }

    const page = await response.json();
    rows.push(...page);
    if (page.length < PAGE) return rows;
  }
}

function sqlLiteral(value) {
  if (value === null || value === undefined) return 'NULL';
  if (typeof value === 'number') return String(value);
  if (typeof value === 'boolean') return value ? 'TRUE' : 'FALSE';
  if (typeof value === 'object') {
    return `'${JSON.stringify(value).replace(/'/g, "''")}'::jsonb`;
  }
  return `'${String(value).replace(/'/g, "''")}'`;
}

function toSql(table, rows) {
  if (!rows.length) return `-- ${table}: no rows\n`;
  const columns = Object.keys(rows[0]);
  const quoted = columns.map((c) => `"${c}"`).join(', ');

  const values = rows
    .map((row) => `  (${columns.map((c) => sqlLiteral(row[c])).join(', ')})`)
    .join(',\n');

  // ON CONFLICT DO NOTHING so a restore into a non-empty database tops it up
  // rather than failing on the first duplicate key.
  return (
    `-- ${table}: ${rows.length} rows\n` +
    `INSERT INTO public."${table}" (${quoted}) VALUES\n${values}\nON CONFLICT DO NOTHING;\n`
  );
}

async function main() {
  const outFlag = process.argv.indexOf('--out');
  const outRoot = outFlag !== -1 ? process.argv[outFlag + 1] : 'backups';

  const { url, key } = loadEnv();
  const stamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, 19);
  const dir = join(outRoot, `supabase-${stamp}`);
  mkdirSync(dir, { recursive: true });

  console.log(`Dumping ${url}`);
  console.log(`  -> ${resolve(dir)}\n`);

  const sqlParts = [
    '-- ViMusic Supabase data dump',
    `-- ${new Date().toISOString()}`,
    '-- Data only. Restore into a database that already has the schema.',
    '',
  ];
  let total = 0;

  for (const table of TABLES) {
    process.stdout.write(`  ${table.padEnd(20)}`);
    let rows;
    try {
      rows = await fetchTable(url, key, table);
    } catch (error) {
      console.log(`FAILED  ${error.message}`);
      continue;
    }

    if (rows === null) {
      console.log('skipped (no such table)');
      continue;
    }

    writeFileSync(join(dir, `${table}.json`), JSON.stringify(rows, null, 2));
    sqlParts.push(toSql(table, rows));
    total += rows.length;
    console.log(`${rows.length} rows`);
  }

  writeFileSync(join(dir, 'restore.sql'), sqlParts.join('\n'));
  console.log(`\nDone. ${total} rows across ${TABLES.length} tables.`);
  console.log(`JSON per table, plus restore.sql you can paste into the SQL editor.`);
}

main().catch((error) => {
  console.error(`\nBackup failed: ${error.message}`);
  process.exit(1);
});
