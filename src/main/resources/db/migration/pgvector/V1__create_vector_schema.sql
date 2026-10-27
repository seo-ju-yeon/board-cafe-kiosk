-- Spring AI 1.1.4 PgVectorStore, text-embedding-3-small (1536 dimensions).
-- 문서 임베딩은 앱에서 별도로 수행한다. 이 마이그레이션은 외부 API를 호출하지 않는다.
CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA public;

CREATE TABLE public.vector_store
(
    id        UUID PRIMARY KEY DEFAULT public.uuid_generate_v4(),
    content   TEXT,
    metadata  JSON,
    embedding public.vector(1536)
);

CREATE INDEX spring_ai_vector_index
    ON public.vector_store USING hnsw (embedding public.vector_cosine_ops);
