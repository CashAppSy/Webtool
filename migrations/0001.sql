CREATE TABLE IF NOT EXISTS entities (kind TEXT NOT NULL, id TEXT NOT NULL, position INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY (kind,id,position));
CREATE TABLE IF NOT EXISTS meta (id INTEGER PRIMARY KEY CHECK(id=1), revision INTEGER NOT NULL DEFAULT 1);
INSERT OR IGNORE INTO meta(id,revision) VALUES(1,1);
CREATE TABLE IF NOT EXISTS push_tokens (hash TEXT PRIMARY KEY, token TEXT NOT NULL, platform TEXT NOT NULL, uid TEXT NOT NULL, updated_at TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS deliveries (notification_id TEXT NOT NULL, token_hash TEXT NOT NULL, status TEXT NOT NULL, updated_at TEXT NOT NULL, PRIMARY KEY(notification_id,token_hash));
CREATE TABLE IF NOT EXISTS audit (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, action TEXT NOT NULL, target TEXT NOT NULL, created_at TEXT NOT NULL);

CREATE TABLE IF NOT EXISTS seed_registry(kind TEXT NOT NULL,id TEXT NOT NULL,PRIMARY KEY(kind,id));

CREATE TABLE IF NOT EXISTS push_errors(notification_id TEXT NOT NULL,token_hash TEXT NOT NULL,platform TEXT NOT NULL,code TEXT NOT NULL,updated_at TEXT NOT NULL,PRIMARY KEY(notification_id,token_hash));
CREATE TABLE IF NOT EXISTS push_campaigns(id TEXT PRIMARY KEY,source_id TEXT NOT NULL,payload TEXT NOT NULL,status TEXT NOT NULL DEFAULT 'pending',created_at TEXT NOT NULL,actor TEXT NOT NULL);
CREATE UNIQUE INDEX IF NOT EXISTS one_pending_resend ON push_campaigns(source_id) WHERE status='pending';
UPDATE meta SET revision=revision+1 WHERE id=1 AND EXISTS(SELECT 1 FROM entities WHERE kind='settings' AND position=0 AND json_valid(payload) AND (json_extract(payload,'$.title_ar')='دليلك إلى خدمات MTN' OR json_extract(payload,'$.title_en')='Your MTN service guide'));
UPDATE entities SET payload=json_set(payload,'$.title_ar',CASE WHEN json_extract(payload,'$.title_ar')='دليلك إلى خدمات MTN' THEN 'Training Hub' ELSE json_extract(payload,'$.title_ar') END,'$.title_en',CASE WHEN json_extract(payload,'$.title_en')='Your MTN service guide' THEN 'Training Hub' ELSE json_extract(payload,'$.title_en') END,'$.footer_ar',CASE WHEN json_extract(payload,'$.footer_ar')='MTN Syria · منصة المعرفة والتدريب' THEN 'Training Hub · منصة المعرفة والتدريب' ELSE json_extract(payload,'$.footer_ar') END,'$.footer_en',CASE WHEN json_extract(payload,'$.footer_en')='MTN Syria · Knowledge & training' THEN 'Training Hub · Knowledge & training' ELSE json_extract(payload,'$.footer_en') END) WHERE kind='settings' AND position=0 AND json_valid(payload) AND (json_extract(payload,'$.title_ar')='دليلك إلى خدمات MTN' OR json_extract(payload,'$.title_en')='Your MTN service guide');
