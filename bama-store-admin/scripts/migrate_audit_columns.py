"""Normalize existing MySQL audit fields using an already authenticated PyMySQL connection.

Call migrate(connection, backup_directory, apply=False) to back up and preview;
apply=True backs up again before executing. Never stores connection credentials.
"""
from pathlib import Path
from datetime import datetime
import hashlib
import json
import re

DEFINITIONS = {
    'create_time': "DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
    'update_time': "DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
    'deleted': "BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标识'",
}


def quote(name):
    if not re.fullmatch(r'[a-zA-Z0-9_]+', name):
        raise ValueError('Unexpected identifier')
    return '`' + name + '`'


def matches(name, col):
    typ, nullable, default, extra, comment = col
    default = str(default).lower().replace('()', '')
    if name == 'create_time':
        return typ == 'datetime' and nullable == 'NO' and default == 'current_timestamp' and 'on update' not in extra.lower() and comment == '创建时间'
    if name == 'update_time':
        return typ == 'datetime' and nullable == 'YES' and default == 'none' and 'on update current_timestamp' in extra.lower() and comment == '更新时间'
    return typ == 'bit(1)' and nullable == 'NO' and default in ("b'0'", '0') and comment == '删除标识'


def columns(cur, table):
    cur.execute("SELECT COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COLUMN_DEFAULT,EXTRA,COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=%s", (table,))
    return {row[0]: row[1:] for row in cur.fetchall()}


def migrate(conn, backup_directory, apply=False):
    backup_directory = Path(backup_directory)
    backup_directory.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now().strftime('%Y%m%d-%H%M%S-%f')
    statements, inventory, counts = [], {}, {}
    with conn.cursor() as cur:
        cur.execute("SET SESSION time_zone='+08:00'")
        cur.execute('SET SESSION lock_wait_timeout=5')
        cur.execute('SET TRANSACTION ISOLATION LEVEL REPEATABLE READ')
        cur.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT')
        cur.execute("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME")
        tables = [r[0] for r in cur.fetchall()]
        if not tables or any(not t.startswith('t_') for t in tables):
            raise ValueError('Expected only project t_ tables in the selected database')
        dump = ["SET NAMES utf8mb4;", "SET time_zone='+08:00';"]
        for table in tables:
            q = quote(table)
            cols = columns(cur, table)
            inventory[table] = cols
            if 'deleted' in cols:
                cur.execute(f'SELECT COUNT(*) FROM {q} WHERE deleted IS NULL OR deleted NOT IN (0,1)')
                if cur.fetchone()[0]:
                    raise ValueError(f'{table}: deletion values cannot be converted safely')
            cur.execute(f'SHOW CREATE TABLE {q}')
            dump.append(cur.fetchone()[1] + ';')
            cur.execute(f'SELECT * FROM {q}')
            names = ','.join(quote(c[0]) for c in cur.description)
            rows = cur.fetchall()
            counts[table] = len(rows)
            for row in rows:
                values = ','.join("X'" + v.hex() + "'" if isinstance(v, bytes) else conn.escape(v) for v in row)
                dump.append(f'INSERT INTO {q} ({names}) VALUES ({values});')
            changes = []
            for name, definition in DEFINITIONS.items():
                if name not in cols:
                    changes.append(f'ADD COLUMN {quote(name)} {definition}')
                elif not matches(name, cols[name]):
                    changes.append(f'MODIFY COLUMN {quote(name)} {definition}')
            if changes:
                if 'create_time' in cols:
                    # Explicit self-assignment preserves historical update timestamps.
                    keep = ', update_time=update_time' if 'update_time' in cols else ''
                    statements.append(f'UPDATE {q} SET create_time=CURRENT_TIMESTAMP{keep} WHERE create_time IS NULL')
                statements.append(f'ALTER TABLE {q} ' + ', '.join(changes))
        conn.commit()
        backup = backup_directory / f'audit-columns-before-{stamp}.sql'
        raw = ('\n'.join(dump) + '\n').encode('utf-8')
        backup.write_bytes(raw)
        assert backup.read_bytes() == raw
        plan = backup_directory / f'audit-columns-plan-{stamp}.sql'
        plan.write_text(';\n'.join(statements) + (';\n' if statements else ''), encoding='utf-8')
        metadata = dict(tables=inventory, rows=counts, backupSha256=hashlib.sha256(raw).hexdigest())
        backup.with_suffix('.json').write_text(json.dumps(metadata, default=str, ensure_ascii=False, indent=2), encoding='utf-8')
        if apply:
            # MySQL DDL commits implicitly. A partial failure is resumable, not transactional.
            for statement in statements:
                cur.execute(statement)
                conn.commit()
            for table in tables:
                actual = columns(cur, table)
                assert all(name in actual and matches(name, actual[name]) for name in DEFINITIONS), table
        return dict(applied=apply, tables=len(tables), statements=len(statements), rows=counts, backup=str(backup), plan=str(plan))
