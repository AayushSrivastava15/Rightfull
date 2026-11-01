--
-- PostgreSQL database dump
--

\restrict 2uZmVVRqOAPrjdYXjTWELIul3ajeXdcmRGPjIN07HqGUZ8elprBdsGPi0ttatUK

-- Dumped from database version 17.11
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

DROP POLICY IF EXISTS workspaces_update_policy ON public.workspaces;
DROP POLICY IF EXISTS workspaces_select_policy ON public.workspaces;
DROP POLICY IF EXISTS workspaces_insert_policy ON public.workspaces;
DROP POLICY IF EXISTS workspaces_delete_policy ON public.workspaces;
DROP POLICY IF EXISTS workspace_members_update_policy ON public.workspace_members;
DROP POLICY IF EXISTS workspace_members_select_policy ON public.workspace_members;
DROP POLICY IF EXISTS workspace_members_insert_policy ON public.workspace_members;
DROP POLICY IF EXISTS workspace_members_delete_policy ON public.workspace_members;
DROP POLICY IF EXISTS workspace_invitations_update_policy ON public.workspace_invitations;
DROP POLICY IF EXISTS workspace_invitations_select_policy ON public.workspace_invitations;
DROP POLICY IF EXISTS workspace_invitations_insert_policy ON public.workspace_invitations;
DROP POLICY IF EXISTS workspace_invitations_delete_policy ON public.workspace_invitations;
DROP POLICY IF EXISTS users_update_policy ON public.users;
DROP POLICY IF EXISTS users_select_policy ON public.users;
DROP POLICY IF EXISTS users_insert_policy ON public.users;
DROP POLICY IF EXISTS user_preferences_all_policy ON public.user_preferences;
DROP POLICY IF EXISTS products_update_policy ON public.products;
DROP POLICY IF EXISTS products_select_policy ON public.products;
DROP POLICY IF EXISTS products_insert_policy ON public.products;
DROP POLICY IF EXISTS products_delete_policy ON public.products;
DROP POLICY IF EXISTS product_signing_keys_update_policy ON public.product_signing_keys;
DROP POLICY IF EXISTS product_signing_keys_select_policy ON public.product_signing_keys;
DROP POLICY IF EXISTS product_signing_keys_insert_policy ON public.product_signing_keys;
DROP POLICY IF EXISTS product_signing_keys_delete_policy ON public.product_signing_keys;
DROP POLICY IF EXISTS audit_logs_select_policy ON public.audit_logs;
DROP POLICY IF EXISTS audit_logs_insert_policy ON public.audit_logs;
ALTER TABLE IF EXISTS ONLY public.workspaces DROP CONSTRAINT IF EXISTS workspaces_owner_id_fkey;
ALTER TABLE IF EXISTS ONLY public.workspace_members DROP CONSTRAINT IF EXISTS workspace_members_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.workspace_members DROP CONSTRAINT IF EXISTS workspace_members_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.workspace_members DROP CONSTRAINT IF EXISTS workspace_members_invited_by_fkey;
ALTER TABLE IF EXISTS ONLY public.workspace_invitations DROP CONSTRAINT IF EXISTS workspace_invitations_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.workspace_invitations DROP CONSTRAINT IF EXISTS workspace_invitations_invited_by_fkey;
ALTER TABLE IF EXISTS ONLY public.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_user_id_fkey;
ALTER TABLE IF EXISTS ONLY public.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_default_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS products_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS products_created_by_fkey;
ALTER TABLE IF EXISTS ONLY public.product_signing_keys DROP CONSTRAINT IF EXISTS product_signing_keys_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.product_signing_keys DROP CONSTRAINT IF EXISTS product_signing_keys_product_id_fkey;
ALTER TABLE IF EXISTS ONLY public.product_signing_keys DROP CONSTRAINT IF EXISTS fk_signing_keys_product_workspace;
ALTER TABLE IF EXISTS ONLY public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_workspace_id_fkey;
ALTER TABLE IF EXISTS ONLY public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_actor_id_fkey;
DROP TRIGGER IF EXISTS trg_audit_logs_prevent_mutation ON public.audit_logs;
DROP INDEX IF EXISTS public.uq_product_signing_keys_one_active;
DROP INDEX IF EXISTS public.idx_workspaces_slug;
DROP INDEX IF EXISTS public.idx_workspaces_owner_id;
DROP INDEX IF EXISTS public.idx_workspace_members_workspace_id;
DROP INDEX IF EXISTS public.idx_workspace_members_user_id;
DROP INDEX IF EXISTS public.idx_workspace_members_role;
DROP INDEX IF EXISTS public.idx_workspace_invitations_workspace_status;
DROP INDEX IF EXISTS public.idx_workspace_invitations_workspace_id;
DROP INDEX IF EXISTS public.idx_workspace_invitations_token_hash;
DROP INDEX IF EXISTS public.idx_workspace_invitations_lookup;
DROP INDEX IF EXISTS public.idx_users_supabase_id;
DROP INDEX IF EXISTS public.idx_users_email;
DROP INDEX IF EXISTS public.idx_user_preferences_user_id;
DROP INDEX IF EXISTS public.idx_user_preferences_default_workspace;
DROP INDEX IF EXISTS public.idx_signing_keys_workspace;
DROP INDEX IF EXISTS public.idx_signing_keys_product_status;
DROP INDEX IF EXISTS public.idx_products_workspace_status;
DROP INDEX IF EXISTS public.idx_products_workspace;
DROP INDEX IF EXISTS public.idx_products_created_at;
DROP INDEX IF EXISTS public.idx_audit_logs_workspace_time;
DROP INDEX IF EXISTS public.idx_audit_logs_target;
DROP INDEX IF EXISTS public.idx_audit_logs_actor;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.workspaces DROP CONSTRAINT IF EXISTS workspaces_slug_key;
ALTER TABLE IF EXISTS ONLY public.workspaces DROP CONSTRAINT IF EXISTS workspaces_pkey;
ALTER TABLE IF EXISTS ONLY public.workspace_members DROP CONSTRAINT IF EXISTS workspace_members_pkey;
ALTER TABLE IF EXISTS ONLY public.workspace_invitations DROP CONSTRAINT IF EXISTS workspace_invitations_token_hash_key;
ALTER TABLE IF EXISTS ONLY public.workspace_invitations DROP CONSTRAINT IF EXISTS workspace_invitations_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_supabase_user_id_key;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE IF EXISTS ONLY public.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_user_id_key;
ALTER TABLE IF EXISTS ONLY public.user_preferences DROP CONSTRAINT IF EXISTS user_preferences_pkey;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS uq_products_workspace_slug;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS uq_products_id_workspace;
ALTER TABLE IF EXISTS ONLY public.product_signing_keys DROP CONSTRAINT IF EXISTS uq_product_key_id;
ALTER TABLE IF EXISTS ONLY public.workspace_members DROP CONSTRAINT IF EXISTS uk_workspace_user;
ALTER TABLE IF EXISTS ONLY public.products DROP CONSTRAINT IF EXISTS products_pkey;
ALTER TABLE IF EXISTS ONLY public.product_signing_keys DROP CONSTRAINT IF EXISTS product_signing_keys_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_pkey;
DROP TABLE IF EXISTS public.workspaces;
DROP TABLE IF EXISTS public.workspace_members;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.user_preferences;
DROP TABLE IF EXISTS public.products;
DROP TABLE IF EXISTS public.product_signing_keys;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP TABLE IF EXISTS public.audit_logs;
DROP FUNCTION IF EXISTS public.trg_audit_logs_immutable();
DROP FUNCTION IF EXISTS public.lookup_invitation_for_redemption(p_token_hash character varying);
DROP TABLE IF EXISTS public.workspace_invitations;
DROP FUNCTION IF EXISTS public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid);
DROP FUNCTION IF EXISTS public.app_current_workspace_id();
DROP FUNCTION IF EXISTS public.app_current_user_id();
DROP FUNCTION IF EXISTS public.app_current_user_email();
DROP FUNCTION IF EXISTS public.app_current_supabase_user_id();
DROP SCHEMA IF EXISTS public;
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: pg_database_owner
--

CREATE SCHEMA public;


ALTER SCHEMA public OWNER TO pg_database_owner;

--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: pg_database_owner
--

COMMENT ON SCHEMA public IS 'standard public schema';


--
-- Name: app_current_supabase_user_id(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.app_current_supabase_user_id() RETURNS uuid
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_supabase_user_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.app_current_supabase_user_id() OWNER TO postgres;

--
-- Name: app_current_user_email(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.app_current_user_email() RETURNS character varying
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    SET search_path TO 'public', 'pg_temp'
    AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_email', true), '');
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.app_current_user_email() OWNER TO postgres;

--
-- Name: app_current_user_id(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.app_current_user_id() RETURNS uuid
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_user_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.app_current_user_id() OWNER TO postgres;

--
-- Name: app_current_workspace_id(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.app_current_workspace_id() RETURNS uuid
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    AS $$
BEGIN
    RETURN NULLIF(current_setting('app.current_workspace_id', true), '')::uuid;
EXCEPTION WHEN OTHERS THEN
    RETURN NULL;
END;
$$;


ALTER FUNCTION public.app_current_workspace_id() OWNER TO postgres;

--
-- Name: app_is_workspace_admin(uuid, uuid); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) RETURNS boolean
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    SET search_path TO 'public', 'pg_temp'
    AS $$
BEGIN
    IF p_workspace_id IS NULL OR p_user_id IS NULL THEN
        RETURN FALSE;
    END IF;
    RETURN EXISTS (
        SELECT 1 FROM workspace_members
        WHERE workspace_id = p_workspace_id
          AND user_id = p_user_id
          AND role IN ('OWNER', 'ADMIN')
    );
END;
$$;


ALTER FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: workspace_invitations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workspace_invitations (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workspace_id uuid NOT NULL,
    email character varying(255) NOT NULL,
    role character varying(32) DEFAULT 'MEMBER'::character varying NOT NULL,
    token_hash character varying(64) NOT NULL,
    invited_by uuid NOT NULL,
    status character varying(32) DEFAULT 'PENDING'::character varying NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    accepted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_workspace_invitations_email_non_empty CHECK ((length(TRIM(BOTH FROM email)) > 0)),
    CONSTRAINT chk_workspace_invitations_role CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'MEMBER'::character varying, 'VIEWER'::character varying])::text[]))),
    CONSTRAINT chk_workspace_invitations_status CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'REVOKED'::character varying])::text[])))
);


ALTER TABLE public.workspace_invitations OWNER TO postgres;

--
-- Name: lookup_invitation_for_redemption(character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) RETURNS SETOF public.workspace_invitations
    LANGUAGE plpgsql STABLE SECURITY DEFINER
    SET search_path TO 'public', 'pg_temp'
    AS $$
BEGIN
    IF p_token_hash IS NULL OR length(trim(p_token_hash)) = 0 THEN
        RETURN;
    END IF;
    RETURN QUERY
    SELECT * FROM workspace_invitations
    WHERE token_hash = p_token_hash
      AND status = 'PENDING'
      AND expires_at > CURRENT_TIMESTAMP;
END;
$$;


ALTER FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) OWNER TO postgres;

--
-- Name: trg_audit_logs_immutable(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_audit_logs_immutable() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable and cannot be updated or deleted';
END;
$$;


ALTER FUNCTION public.trg_audit_logs_immutable() OWNER TO postgres;

--
-- Name: audit_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.audit_logs (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workspace_id uuid,
    actor_id uuid,
    event_type character varying(64) NOT NULL,
    target_type character varying(64),
    target_id uuid,
    ip_address character varying(45),
    details jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.audit_logs OWNER TO postgres;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO postgres;

--
-- Name: product_signing_keys; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_signing_keys (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workspace_id uuid NOT NULL,
    product_id uuid NOT NULL,
    key_id character varying(64) NOT NULL,
    algorithm character varying(32) DEFAULT 'ED25519'::character varying NOT NULL,
    public_key text NOT NULL,
    encrypted_private_key bytea NOT NULL,
    encryption_nonce bytea NOT NULL,
    encryption_version character varying(16) DEFAULT 'v1'::character varying NOT NULL,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    rotated_at timestamp with time zone,
    retired_at timestamp with time zone,
    CONSTRAINT chk_signing_keys_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'RETIRED'::character varying, 'REVOKED'::character varying])::text[])))
);


ALTER TABLE public.product_signing_keys OWNER TO postgres;

--
-- Name: products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.products (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workspace_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    slug character varying(100) NOT NULL,
    description text,
    version character varying(50),
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_by uuid,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archived_at timestamp with time zone,
    CONSTRAINT chk_products_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ARCHIVED'::character varying])::text[])))
);


ALTER TABLE public.products OWNER TO postgres;

--
-- Name: user_preferences; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.user_preferences (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    user_id uuid NOT NULL,
    default_workspace_id uuid,
    theme character varying(32) DEFAULT 'dark'::character varying NOT NULL,
    email_notifications boolean DEFAULT true NOT NULL,
    ui_preferences jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_user_preferences_theme CHECK (((theme)::text = ANY ((ARRAY['light'::character varying, 'dark'::character varying, 'system'::character varying])::text[])))
);


ALTER TABLE public.user_preferences OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    supabase_user_id uuid NOT NULL,
    email character varying(255) NOT NULL,
    full_name character varying(255),
    avatar_url text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_users_email_non_empty CHECK ((length(TRIM(BOTH FROM email)) > 0))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: workspace_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workspace_members (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    workspace_id uuid NOT NULL,
    user_id uuid NOT NULL,
    role character varying(32) DEFAULT 'MEMBER'::character varying NOT NULL,
    invited_by uuid,
    joined_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_workspace_members_role CHECK (((role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying, 'MEMBER'::character varying, 'VIEWER'::character varying])::text[])))
);


ALTER TABLE public.workspace_members OWNER TO postgres;

--
-- Name: workspaces; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workspaces (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(128) NOT NULL,
    slug character varying(64) NOT NULL,
    owner_id uuid NOT NULL,
    tier character varying(32) DEFAULT 'COMMUNITY'::character varying NOT NULL,
    status character varying(32) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_workspaces_slug_format CHECK (((slug)::text ~ '^[a-z0-9-]+$'::text)),
    CONSTRAINT chk_workspaces_status CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'SUSPENDED'::character varying, 'ARCHIVED'::character varying])::text[]))),
    CONSTRAINT chk_workspaces_tier CHECK (((tier)::text = ANY ((ARRAY['COMMUNITY'::character varying, 'PRO'::character varying, 'ENTERPRISE'::character varying])::text[])))
);


ALTER TABLE public.workspaces OWNER TO postgres;

--
-- Data for Name: audit_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.audit_logs (id, workspace_id, actor_id, event_type, target_type, target_id, ip_address, details, created_at) FROM stdin;
2968e945-6ec7-4ceb-9f61-fc4518558e55	9c3a76f2-e313-4167-86ba-27f856c7a2bb	48de1ad1-8ca8-4024-9a62-3f812e19ddb0	WORKSPACE_CREATED	WORKSPACE	\N	\N	{}	2026-10-31 17:32:02.555494+00
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init core schema	SQL	V1__init_core_schema.sql	-850693739	postgres	2026-10-30 17:29:47.708849	853	t
2	2	workspace invitations	SQL	V2__workspace_invitations.sql	-81613785	postgres	2026-10-31 16:23:42.140898	450	t
3	3	audit logs and rls	SQL	V3__audit_logs_and_rls.sql	-1310822808	postgres	2026-10-31 17:31:35.992122	1823	t
4	4	remediate rls policies	SQL	V4__remediate_rls_policies.sql	63681462	postgres	2026-11-01 15:21:59.295588	1937	t
5	5	product catalog and signing keys	SQL	V5__product_catalog_and_signing_keys.sql	-1630381995	postgres	2026-11-01 15:22:02.78482	3838	t
\.


--
-- Data for Name: product_signing_keys; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.product_signing_keys (id, workspace_id, product_id, key_id, algorithm, public_key, encrypted_private_key, encryption_nonce, encryption_version, status, created_at, rotated_at, retired_at) FROM stdin;
\.


--
-- Data for Name: products; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.products (id, workspace_id, name, slug, description, version, status, created_by, created_at, updated_at, archived_at) FROM stdin;
\.


--
-- Data for Name: user_preferences; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.user_preferences (id, user_id, default_workspace_id, theme, email_notifications, ui_preferences, created_at, updated_at) FROM stdin;
879c4656-01a1-4d82-8816-8e7afc43f879	f838693c-9305-4cae-9ba9-4c2eead7909b	3d5eb110-9561-4c07-8015-384e29647e9b	dark	t	{}	2026-10-31 17:18:35.84526+00	2026-10-31 17:18:35.84526+00
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, supabase_user_id, email, full_name, avatar_url, is_active, created_at, updated_at) FROM stdin;
f838693c-9305-4cae-9ba9-4c2eead7909b	d4779269-a320-4331-ad6d-ca4b60553b9d	duelistaws33@gmail.com	Aayush Srivastava	https://lh3.googleusercontent.com/a/ACg8ocJCCiXMfFpeayRlobA4N6h-nJ1f0ECQ7Tuoqe_3TWXMTfHm2g=s96-c	t	2026-10-31 17:18:35.665216+00	2026-10-31 17:18:35.665216+00
48de1ad1-8ca8-4024-9a62-3f812e19ddb0	f2d1fcdf-577d-4a38-9dcc-6114943497b1	alpha-48de1ad1-8ca8-4024-9a62-3f812e19ddb0@rightfull.test	Tenant Alpha Owner	\N	t	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
26a72fff-1759-42a6-b1b6-fe40ffc4c92e	8a54b2d7-886f-4fd7-8de2-467a82a1aec0	beta-26a72fff-1759-42a6-b1b6-fe40ffc4c92e@rightfull.test	Tenant Beta Owner	\N	t	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
\.


--
-- Data for Name: workspace_invitations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.workspace_invitations (id, workspace_id, email, role, token_hash, invited_by, status, expires_at, accepted_at, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: workspace_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.workspace_members (id, workspace_id, user_id, role, invited_by, joined_at, updated_at) FROM stdin;
b9e30f98-b5f8-47af-93d8-fc71ece0991c	3d5eb110-9561-4c07-8015-384e29647e9b	f838693c-9305-4cae-9ba9-4c2eead7909b	OWNER	\N	2026-10-31 17:18:35.807097+00	2026-10-31 17:18:35.807097+00
f759acb3-9b2b-432a-8615-fbf0251c9998	9c3a76f2-e313-4167-86ba-27f856c7a2bb	48de1ad1-8ca8-4024-9a62-3f812e19ddb0	OWNER	\N	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
4e09d885-bb97-4c70-b4de-dd3b3afeb48f	bfe63eeb-d479-412b-a234-96ba2a99813e	26a72fff-1759-42a6-b1b6-fe40ffc4c92e	OWNER	\N	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
\.


--
-- Data for Name: workspaces; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.workspaces (id, name, slug, owner_id, tier, status, created_at, updated_at) FROM stdin;
3d5eb110-9561-4c07-8015-384e29647e9b	Aayush Srivastava's Workspace	aayush-srivastava-workspace	f838693c-9305-4cae-9ba9-4c2eead7909b	COMMUNITY	ACTIVE	2026-10-31 17:18:35.76785+00	2026-10-31 17:18:35.76785+00
9c3a76f2-e313-4167-86ba-27f856c7a2bb	Workspace Alpha	ws-alpha-9c3a76f2	48de1ad1-8ca8-4024-9a62-3f812e19ddb0	COMMUNITY	ACTIVE	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
bfe63eeb-d479-412b-a234-96ba2a99813e	Workspace Beta	ws-beta-bfe63eeb	26a72fff-1759-42a6-b1b6-fe40ffc4c92e	COMMUNITY	ACTIVE	2026-10-31 17:31:58.813246+00	2026-10-31 17:31:58.813246+00
\.


--
-- Name: audit_logs audit_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: product_signing_keys product_signing_keys_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_signing_keys
    ADD CONSTRAINT product_signing_keys_pkey PRIMARY KEY (id);


--
-- Name: products products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY (id);


--
-- Name: workspace_members uk_workspace_user; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT uk_workspace_user UNIQUE (workspace_id, user_id);


--
-- Name: product_signing_keys uq_product_key_id; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_signing_keys
    ADD CONSTRAINT uq_product_key_id UNIQUE (product_id, key_id);


--
-- Name: products uq_products_id_workspace; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT uq_products_id_workspace UNIQUE (id, workspace_id);


--
-- Name: products uq_products_workspace_slug; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT uq_products_workspace_slug UNIQUE (workspace_id, slug);


--
-- Name: user_preferences user_preferences_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_pkey PRIMARY KEY (id);


--
-- Name: user_preferences user_preferences_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_user_id_key UNIQUE (user_id);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_supabase_user_id_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_supabase_user_id_key UNIQUE (supabase_user_id);


--
-- Name: workspace_invitations workspace_invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_invitations
    ADD CONSTRAINT workspace_invitations_pkey PRIMARY KEY (id);


--
-- Name: workspace_invitations workspace_invitations_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_invitations
    ADD CONSTRAINT workspace_invitations_token_hash_key UNIQUE (token_hash);


--
-- Name: workspace_members workspace_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_pkey PRIMARY KEY (id);


--
-- Name: workspaces workspaces_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT workspaces_pkey PRIMARY KEY (id);


--
-- Name: workspaces workspaces_slug_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT workspaces_slug_key UNIQUE (slug);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_audit_logs_actor; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_actor ON public.audit_logs USING btree (actor_id, created_at DESC);


--
-- Name: idx_audit_logs_target; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_target ON public.audit_logs USING btree (workspace_id, target_type, target_id);


--
-- Name: idx_audit_logs_workspace_time; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_audit_logs_workspace_time ON public.audit_logs USING btree (workspace_id, created_at DESC);


--
-- Name: idx_products_created_at; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_created_at ON public.products USING btree (workspace_id, created_at DESC);


--
-- Name: idx_products_workspace; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_workspace ON public.products USING btree (workspace_id);


--
-- Name: idx_products_workspace_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_products_workspace_status ON public.products USING btree (workspace_id, status);


--
-- Name: idx_signing_keys_product_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signing_keys_product_status ON public.product_signing_keys USING btree (product_id, status);


--
-- Name: idx_signing_keys_workspace; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_signing_keys_workspace ON public.product_signing_keys USING btree (workspace_id);


--
-- Name: idx_user_preferences_default_workspace; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_preferences_default_workspace ON public.user_preferences USING btree (default_workspace_id);


--
-- Name: idx_user_preferences_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_user_preferences_user_id ON public.user_preferences USING btree (user_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_supabase_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_supabase_id ON public.users USING btree (supabase_user_id);


--
-- Name: idx_workspace_invitations_lookup; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_invitations_lookup ON public.workspace_invitations USING btree (workspace_id, email);


--
-- Name: idx_workspace_invitations_token_hash; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_workspace_invitations_token_hash ON public.workspace_invitations USING btree (token_hash);


--
-- Name: idx_workspace_invitations_workspace_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_invitations_workspace_id ON public.workspace_invitations USING btree (workspace_id);


--
-- Name: idx_workspace_invitations_workspace_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_invitations_workspace_status ON public.workspace_invitations USING btree (workspace_id, status);


--
-- Name: idx_workspace_members_role; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_members_role ON public.workspace_members USING btree (workspace_id, role);


--
-- Name: idx_workspace_members_user_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_members_user_id ON public.workspace_members USING btree (user_id);


--
-- Name: idx_workspace_members_workspace_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspace_members_workspace_id ON public.workspace_members USING btree (workspace_id);


--
-- Name: idx_workspaces_owner_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_workspaces_owner_id ON public.workspaces USING btree (owner_id);


--
-- Name: idx_workspaces_slug; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_workspaces_slug ON public.workspaces USING btree (slug);


--
-- Name: uq_product_signing_keys_one_active; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_product_signing_keys_one_active ON public.product_signing_keys USING btree (product_id) WHERE ((status)::text = 'ACTIVE'::text);


--
-- Name: audit_logs trg_audit_logs_prevent_mutation; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_audit_logs_prevent_mutation BEFORE DELETE OR UPDATE ON public.audit_logs FOR EACH ROW EXECUTE FUNCTION public.trg_audit_logs_immutable();


--
-- Name: audit_logs audit_logs_actor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: audit_logs audit_logs_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.audit_logs
    ADD CONSTRAINT audit_logs_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- Name: product_signing_keys fk_signing_keys_product_workspace; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_signing_keys
    ADD CONSTRAINT fk_signing_keys_product_workspace FOREIGN KEY (product_id, workspace_id) REFERENCES public.products(id, workspace_id) ON DELETE CASCADE;


--
-- Name: product_signing_keys product_signing_keys_product_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_signing_keys
    ADD CONSTRAINT product_signing_keys_product_id_fkey FOREIGN KEY (product_id) REFERENCES public.products(id) ON DELETE CASCADE;


--
-- Name: product_signing_keys product_signing_keys_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_signing_keys
    ADD CONSTRAINT product_signing_keys_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- Name: products products_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: products products_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- Name: user_preferences user_preferences_default_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_default_workspace_id_fkey FOREIGN KEY (default_workspace_id) REFERENCES public.workspaces(id) ON DELETE SET NULL;


--
-- Name: user_preferences user_preferences_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.user_preferences
    ADD CONSTRAINT user_preferences_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: workspace_invitations workspace_invitations_invited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_invitations
    ADD CONSTRAINT workspace_invitations_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: workspace_invitations workspace_invitations_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_invitations
    ADD CONSTRAINT workspace_invitations_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- Name: workspace_members workspace_members_invited_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_invited_by_fkey FOREIGN KEY (invited_by) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: workspace_members workspace_members_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: workspace_members workspace_members_workspace_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspace_members
    ADD CONSTRAINT workspace_members_workspace_id_fkey FOREIGN KEY (workspace_id) REFERENCES public.workspaces(id) ON DELETE CASCADE;


--
-- Name: workspaces workspaces_owner_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workspaces
    ADD CONSTRAINT workspaces_owner_id_fkey FOREIGN KEY (owner_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: audit_logs; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

--
-- Name: audit_logs audit_logs_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY audit_logs_insert_policy ON public.audit_logs FOR INSERT WITH CHECK (((workspace_id = public.app_current_workspace_id()) OR (workspace_id IS NULL)));


--
-- Name: audit_logs audit_logs_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY audit_logs_select_policy ON public.audit_logs FOR SELECT USING ((workspace_id = public.app_current_workspace_id()));


--
-- Name: product_signing_keys; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.product_signing_keys ENABLE ROW LEVEL SECURITY;

--
-- Name: product_signing_keys product_signing_keys_delete_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY product_signing_keys_delete_policy ON public.product_signing_keys FOR DELETE USING (false);


--
-- Name: product_signing_keys product_signing_keys_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY product_signing_keys_insert_policy ON public.product_signing_keys FOR INSERT WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: product_signing_keys product_signing_keys_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY product_signing_keys_select_policy ON public.product_signing_keys FOR SELECT USING ((workspace_id = public.app_current_workspace_id()));


--
-- Name: product_signing_keys product_signing_keys_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY product_signing_keys_update_policy ON public.product_signing_keys FOR UPDATE USING ((workspace_id = public.app_current_workspace_id())) WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: products; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;

--
-- Name: products products_delete_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY products_delete_policy ON public.products FOR DELETE USING (false);


--
-- Name: products products_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY products_insert_policy ON public.products FOR INSERT WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: products products_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY products_select_policy ON public.products FOR SELECT USING ((workspace_id = public.app_current_workspace_id()));


--
-- Name: products products_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY products_update_policy ON public.products FOR UPDATE USING ((workspace_id = public.app_current_workspace_id())) WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: user_preferences; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.user_preferences ENABLE ROW LEVEL SECURITY;

--
-- Name: user_preferences user_preferences_all_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY user_preferences_all_policy ON public.user_preferences USING ((user_id = public.app_current_user_id())) WITH CHECK ((user_id = public.app_current_user_id()));


--
-- Name: users; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

--
-- Name: users users_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY users_insert_policy ON public.users FOR INSERT WITH CHECK (((supabase_user_id = public.app_current_supabase_user_id()) OR (id = public.app_current_user_id()) OR (public.app_current_user_id() IS NULL)));


--
-- Name: users users_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY users_select_policy ON public.users FOR SELECT USING (((id = public.app_current_user_id()) OR (supabase_user_id = public.app_current_supabase_user_id()) OR (id IN ( SELECT wm.user_id
   FROM public.workspace_members wm
  WHERE (wm.workspace_id = public.app_current_workspace_id())))));


--
-- Name: users users_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY users_update_policy ON public.users FOR UPDATE USING ((id = public.app_current_user_id())) WITH CHECK ((id = public.app_current_user_id()));


--
-- Name: workspace_invitations; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.workspace_invitations ENABLE ROW LEVEL SECURITY;

--
-- Name: workspace_invitations workspace_invitations_delete_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_invitations_delete_policy ON public.workspace_invitations FOR DELETE USING ((workspace_id = public.app_current_workspace_id()));


--
-- Name: workspace_invitations workspace_invitations_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_invitations_insert_policy ON public.workspace_invitations FOR INSERT WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: workspace_invitations workspace_invitations_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_invitations_select_policy ON public.workspace_invitations FOR SELECT USING (((workspace_id = public.app_current_workspace_id()) OR ((email)::text = (public.app_current_user_email())::text)));


--
-- Name: workspace_invitations workspace_invitations_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_invitations_update_policy ON public.workspace_invitations FOR UPDATE USING (((workspace_id = public.app_current_workspace_id()) OR ((email)::text = (public.app_current_user_email())::text))) WITH CHECK (((workspace_id = public.app_current_workspace_id()) OR ((status)::text = ANY ((ARRAY['ACCEPTED'::character varying, 'REJECTED'::character varying])::text[]))));


--
-- Name: workspace_members; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.workspace_members ENABLE ROW LEVEL SECURITY;

--
-- Name: workspace_members workspace_members_delete_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_members_delete_policy ON public.workspace_members FOR DELETE USING (((workspace_id = public.app_current_workspace_id()) OR (user_id = public.app_current_user_id())));


--
-- Name: workspace_members workspace_members_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_members_insert_policy ON public.workspace_members FOR INSERT WITH CHECK (((EXISTS ( SELECT 1
   FROM public.workspaces w
  WHERE ((w.id = workspace_members.workspace_id) AND (w.owner_id = public.app_current_user_id()) AND (workspace_members.user_id = public.app_current_user_id())))) OR ((workspace_id = public.app_current_workspace_id()) AND ((role)::text <> 'OWNER'::text) AND public.app_is_workspace_admin(public.app_current_workspace_id(), public.app_current_user_id())) OR (((role)::text <> 'OWNER'::text) AND (EXISTS ( SELECT 1
   FROM public.workspace_invitations wi
  WHERE ((wi.workspace_id = workspace_members.workspace_id) AND ((wi.email)::text = (public.app_current_user_email())::text) AND ((wi.status)::text = 'PENDING'::text) AND ((wi.role)::text = (workspace_members.role)::text)))))));


--
-- Name: workspace_members workspace_members_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_members_select_policy ON public.workspace_members FOR SELECT USING (((workspace_id = public.app_current_workspace_id()) OR (user_id = public.app_current_user_id())));


--
-- Name: workspace_members workspace_members_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspace_members_update_policy ON public.workspace_members FOR UPDATE USING ((workspace_id = public.app_current_workspace_id())) WITH CHECK ((workspace_id = public.app_current_workspace_id()));


--
-- Name: workspaces; Type: ROW SECURITY; Schema: public; Owner: postgres
--

ALTER TABLE public.workspaces ENABLE ROW LEVEL SECURITY;

--
-- Name: workspaces workspaces_delete_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspaces_delete_policy ON public.workspaces FOR DELETE USING (((owner_id = public.app_current_user_id()) AND (id = public.app_current_workspace_id())));


--
-- Name: workspaces workspaces_insert_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspaces_insert_policy ON public.workspaces FOR INSERT WITH CHECK ((owner_id = public.app_current_user_id()));


--
-- Name: workspaces workspaces_select_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspaces_select_policy ON public.workspaces FOR SELECT USING (((id = public.app_current_workspace_id()) OR (owner_id = public.app_current_user_id()) OR (id IN ( SELECT wm.workspace_id
   FROM public.workspace_members wm
  WHERE (wm.user_id = public.app_current_user_id())))));


--
-- Name: workspaces workspaces_update_policy; Type: POLICY; Schema: public; Owner: postgres
--

CREATE POLICY workspaces_update_policy ON public.workspaces FOR UPDATE USING (((id = public.app_current_workspace_id()) AND ((owner_id = public.app_current_user_id()) OR (id IN ( SELECT wm.workspace_id
   FROM public.workspace_members wm
  WHERE ((wm.user_id = public.app_current_user_id()) AND ((wm.role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying])::text[])))))))) WITH CHECK (((id = public.app_current_workspace_id()) AND ((owner_id = public.app_current_user_id()) OR (id IN ( SELECT wm.workspace_id
   FROM public.workspace_members wm
  WHERE ((wm.user_id = public.app_current_user_id()) AND ((wm.role)::text = ANY ((ARRAY['OWNER'::character varying, 'ADMIN'::character varying])::text[]))))))));


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT USAGE ON SCHEMA public TO postgres;
GRANT USAGE ON SCHEMA public TO anon;
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT USAGE ON SCHEMA public TO service_role;
GRANT USAGE ON SCHEMA public TO rightfull_app;


--
-- Name: FUNCTION app_current_supabase_user_id(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.app_current_supabase_user_id() TO anon;
GRANT ALL ON FUNCTION public.app_current_supabase_user_id() TO authenticated;
GRANT ALL ON FUNCTION public.app_current_supabase_user_id() TO service_role;


--
-- Name: FUNCTION app_current_user_email(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.app_current_user_email() TO anon;
GRANT ALL ON FUNCTION public.app_current_user_email() TO authenticated;
GRANT ALL ON FUNCTION public.app_current_user_email() TO service_role;


--
-- Name: FUNCTION app_current_user_id(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.app_current_user_id() TO anon;
GRANT ALL ON FUNCTION public.app_current_user_id() TO authenticated;
GRANT ALL ON FUNCTION public.app_current_user_id() TO service_role;


--
-- Name: FUNCTION app_current_workspace_id(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.app_current_workspace_id() TO anon;
GRANT ALL ON FUNCTION public.app_current_workspace_id() TO authenticated;
GRANT ALL ON FUNCTION public.app_current_workspace_id() TO service_role;


--
-- Name: FUNCTION app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) FROM PUBLIC;
GRANT ALL ON FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) TO anon;
GRANT ALL ON FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) TO authenticated;
GRANT ALL ON FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) TO service_role;
GRANT ALL ON FUNCTION public.app_is_workspace_admin(p_workspace_id uuid, p_user_id uuid) TO rightfull_app;


--
-- Name: TABLE workspace_invitations; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.workspace_invitations TO anon;
GRANT ALL ON TABLE public.workspace_invitations TO authenticated;
GRANT ALL ON TABLE public.workspace_invitations TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.workspace_invitations TO rightfull_app;


--
-- Name: FUNCTION lookup_invitation_for_redemption(p_token_hash character varying); Type: ACL; Schema: public; Owner: postgres
--

REVOKE ALL ON FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) FROM PUBLIC;
GRANT ALL ON FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) TO anon;
GRANT ALL ON FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) TO authenticated;
GRANT ALL ON FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) TO service_role;
GRANT ALL ON FUNCTION public.lookup_invitation_for_redemption(p_token_hash character varying) TO rightfull_app;


--
-- Name: FUNCTION trg_audit_logs_immutable(); Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON FUNCTION public.trg_audit_logs_immutable() TO anon;
GRANT ALL ON FUNCTION public.trg_audit_logs_immutable() TO authenticated;
GRANT ALL ON FUNCTION public.trg_audit_logs_immutable() TO service_role;


--
-- Name: TABLE audit_logs; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.audit_logs TO anon;
GRANT ALL ON TABLE public.audit_logs TO authenticated;
GRANT ALL ON TABLE public.audit_logs TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.audit_logs TO rightfull_app;


--
-- Name: TABLE flyway_schema_history; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.flyway_schema_history TO anon;
GRANT ALL ON TABLE public.flyway_schema_history TO authenticated;
GRANT ALL ON TABLE public.flyway_schema_history TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.flyway_schema_history TO rightfull_app;


--
-- Name: TABLE product_signing_keys; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.product_signing_keys TO anon;
GRANT ALL ON TABLE public.product_signing_keys TO authenticated;
GRANT ALL ON TABLE public.product_signing_keys TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.product_signing_keys TO rightfull_app;


--
-- Name: TABLE products; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.products TO anon;
GRANT ALL ON TABLE public.products TO authenticated;
GRANT ALL ON TABLE public.products TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.products TO rightfull_app;


--
-- Name: TABLE user_preferences; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.user_preferences TO anon;
GRANT ALL ON TABLE public.user_preferences TO authenticated;
GRANT ALL ON TABLE public.user_preferences TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.user_preferences TO rightfull_app;


--
-- Name: TABLE users; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.users TO anon;
GRANT ALL ON TABLE public.users TO authenticated;
GRANT ALL ON TABLE public.users TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.users TO rightfull_app;


--
-- Name: TABLE workspace_members; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.workspace_members TO anon;
GRANT ALL ON TABLE public.workspace_members TO authenticated;
GRANT ALL ON TABLE public.workspace_members TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.workspace_members TO rightfull_app;


--
-- Name: TABLE workspaces; Type: ACL; Schema: public; Owner: postgres
--

GRANT ALL ON TABLE public.workspaces TO anon;
GRANT ALL ON TABLE public.workspaces TO authenticated;
GRANT ALL ON TABLE public.workspaces TO service_role;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE public.workspaces TO rightfull_app;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON SEQUENCES TO service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,USAGE ON SEQUENCES TO rightfull_app;


--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON SEQUENCES TO service_role;


--
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON FUNCTIONS TO service_role;


--
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON FUNCTIONS TO service_role;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT ALL ON TABLES TO service_role;
ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES TO rightfull_app;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: supabase_admin
--

ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO postgres;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO anon;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO authenticated;
ALTER DEFAULT PRIVILEGES FOR ROLE supabase_admin IN SCHEMA public GRANT ALL ON TABLES TO service_role;


--
-- PostgreSQL database dump complete
--

\unrestrict 2uZmVVRqOAPrjdYXjTWELIul3ajeXdcmRGPjIN07HqGUZ8elprBdsGPi0ttatUK

