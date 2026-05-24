"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { AlertCircle, Building2, MessageSquare, Search as SearchIcon, Tag as TagIcon, User as UserIcon } from "lucide-react";
import { Input } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { globalSearch, SearchUnavailableError } from "@/lib/api";
import type { GlobalSearchResult, SearchHit } from "@/lib/api";

const DEBOUNCE_MS = 300;
const MIN_QUERY_LENGTH = 2;
const RETRY_FALLBACK_SECONDS = 5;

type Status = "idle" | "loading" | "ok" | "bootstrap" | "error";

interface SectionConfig {
  key: keyof Omit<GlobalSearchResult, "query">;
  title: string;
  href: (id: string) => string;
  icon: React.ReactNode;
  showAllHref: (q: string) => string;
}

export function SearchClient() {
  const t = useTranslations();
  const S = t.search;

  const inputRef = useRef<HTMLInputElement | null>(null);
  const [query, setQuery] = useState("");
  const [debounced, setDebounced] = useState("");
  const [result, setResult] = useState<GlobalSearchResult | null>(null);
  const [status, setStatus] = useState<Status>("idle");
  const [retryAt, setRetryAt] = useState<number | null>(null);

  // Auto-focus on mount
  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  // 300 ms debounce
  useEffect(() => {
    const id = setTimeout(() => setDebounced(query), DEBOUNCE_MS);
    return () => clearTimeout(id);
  }, [query]);

  const fetchSearch = useCallback(async (q: string) => {
    setStatus("loading");
    try {
      const data = await globalSearch(q);
      setResult(data);
      setStatus("ok");
      setRetryAt(null);
    } catch (e) {
      if (e instanceof SearchUnavailableError) {
        setStatus("bootstrap");
        setResult(null);
        setRetryAt(Date.now() + (e.retryAfterSeconds || RETRY_FALLBACK_SECONDS) * 1000);
      } else {
        setStatus("error");
        setResult(null);
        setRetryAt(null);
      }
    }
  }, []);

  useEffect(() => {
    const trimmed = debounced.trim();
    if (trimmed.length < MIN_QUERY_LENGTH) {
      setResult(null);
      setStatus("idle");
      return;
    }
    void fetchSearch(trimmed);
  }, [debounced, fetchSearch]);

  // Auto-retry on bootstrap-503
  useEffect(() => {
    if (status !== "bootstrap" || retryAt == null) return;
    const delay = Math.max(500, retryAt - Date.now());
    const id = setTimeout(() => {
      const trimmed = debounced.trim();
      if (trimmed.length >= MIN_QUERY_LENGTH) {
        void fetchSearch(trimmed);
      }
    }, delay);
    return () => clearTimeout(id);
  }, [status, retryAt, debounced, fetchSearch]);

  const sections: SectionConfig[] = useMemo(() => [
    {
      key: "companies",
      title: S.sectionCompanies,
      href: (id) => `/companies/${id}`,
      icon: <Building2 className="h-4 w-4 text-oe-gray-mid" />,
      showAllHref: (q) => `/companies?search=${encodeURIComponent(q)}`,
    },
    {
      key: "contacts",
      title: S.sectionContacts,
      href: (id) => `/contacts/${id}`,
      icon: <UserIcon className="h-4 w-4 text-oe-gray-mid" />,
      showAllHref: (q) => `/contacts?search=${encodeURIComponent(q)}`,
    },
    {
      key: "tags",
      title: S.sectionTags,
      href: (id) => `/tags/${id}`,
      icon: <TagIcon className="h-4 w-4 text-oe-gray-mid" />,
      showAllHref: (q) => `/tags?search=${encodeURIComponent(q)}`,
    },
    {
      key: "comments",
      title: S.sectionComments,
      href: () => "#",
      icon: <MessageSquare className="h-4 w-4 text-oe-gray-mid" />,
      showAllHref: (q) => `/updates?search=${encodeURIComponent(q)}`,
    },
  ], [S.sectionCompanies, S.sectionContacts, S.sectionTags, S.sectionComments]);

  const totalHits = result
    ? result.companies.length + result.contacts.length + result.tags.length + result.comments.length
    : 0;

  return (
    <div className="mx-auto max-w-3xl">
      <h1 className="font-heading text-2xl text-oe-dark mb-4 flex items-center gap-2">
        <SearchIcon className="h-6 w-6" />
        {S.title}
      </h1>

      <Input
        ref={inputRef}
        type="search"
        placeholder={S.inputPlaceholder}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        className="text-base"
        aria-label={S.title}
      />

      {status === "bootstrap" && (
        <div role="status" className="mt-4 rounded border border-oe-gray-light bg-oe-gray-lighter p-3 text-sm text-oe-dark">
          {S.bootstrapBanner}
        </div>
      )}

      {status === "error" && (
        <div role="alert" className="mt-4 rounded border border-oe-red bg-oe-red-lighter p-3 text-sm text-oe-red flex items-center gap-2">
          <AlertCircle className="h-4 w-4" />
          {S.errorBanner}
        </div>
      )}

      {status === "idle" && debounced.trim().length < MIN_QUERY_LENGTH && (
        <p className="mt-4 text-sm text-oe-gray-mid">{S.minChars}</p>
      )}

      {status === "ok" && result && totalHits === 0 && (
        <p className="mt-4 text-sm text-oe-gray-mid">{S.emptyResults}</p>
      )}

      {status === "ok" && result && totalHits > 0 && (
        <div className="mt-6 space-y-6">
          {sections.map((section) => {
            const hits = result[section.key];
            if (hits.length === 0) return null;
            return (
              <Section
                key={section.key}
                title={section.title}
                showAllLabel={S.showAll}
                showAllHref={section.showAllHref(result.query)}
                hits={hits}
                icon={section.icon}
                hrefForHit={(hit) => hrefForHit(hit, section.key, section.href)}
              />
            );
          })}
        </div>
      )}
    </div>
  );
}

function hrefForHit(
  hit: SearchHit,
  section: keyof Omit<GlobalSearchResult, "query">,
  defaultHref: (id: string) => string,
): string {
  if (section === "comments" && hit.ownerType && hit.ownerId) {
    if (hit.ownerType === "company") return `/companies/${hit.ownerId}`;
    if (hit.ownerType === "contact") return `/contacts/${hit.ownerId}`;
    if (hit.ownerType === "task") return `/tasks/${hit.ownerId}`;
  }
  return defaultHref(hit.id);
}

interface SectionProps {
  title: string;
  showAllLabel: string;
  showAllHref: string;
  hits: SearchHit[];
  icon: React.ReactNode;
  hrefForHit: (hit: SearchHit) => string;
}

function Section({ title, showAllLabel, showAllHref, hits, icon, hrefForHit }: SectionProps) {
  return (
    <section>
      <div className="mb-2 flex items-baseline justify-between border-b border-oe-gray-light pb-1">
        <h2 className="font-heading text-lg text-oe-dark flex items-center gap-2">
          {icon}
          {title}
          <span className="text-sm text-oe-gray-mid font-normal">({hits.length})</span>
        </h2>
        <Link href={showAllHref} className="text-sm text-oe-blue hover:underline">
          {showAllLabel}
        </Link>
      </div>
      <ul className="divide-y divide-oe-gray-light">
        {hits.map((hit) => (
          <li key={hit.id}>
            <Link
              href={hrefForHit(hit)}
              className="block py-2 hover:bg-oe-gray-lighter px-2 -mx-2 rounded"
            >
              <div
                className="font-medium text-oe-dark"
                dangerouslySetInnerHTML={{ __html: hit.highlight ?? hit.label }}
              />
              {hit.snippet && (
                <div className="text-sm text-oe-gray-mid truncate">{hit.snippet}</div>
              )}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
