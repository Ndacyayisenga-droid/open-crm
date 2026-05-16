"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { AlertCircle, Bell } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Skeleton,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { getUpdates } from "@/lib/api";
import type { UpdateEntryDto, UpdateType } from "@/lib/types";

export const PAGE_SIZE_OPTIONS = [20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "updates.pageSize";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = window.localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  return DEFAULT_PAGE_SIZE;
}

interface EventTemplates {
  readonly created: string;
  readonly updated: string;
  readonly deleted: string;
}

interface MessageContext {
  readonly type: UpdateType;
  readonly entityId: string | null;
  readonly entityName: string | null;
  readonly templates: {
    readonly company: EventTemplates;
    readonly contact: EventTemplates;
    readonly companyComment: EventTemplates;
    readonly contactComment: EventTemplates;
  };
}

/**
 * Returns `{ before, link, after }` where `link.text` is the entity-name slot of the message,
 * or null when the message should be plain text (deleted entity, or unresolved name).
 */
function renderTemplate(ctx: MessageContext): { before: string; link: { text: string; href: string } | null; after: string } {
  const template = pickTemplate(ctx);
  if (!template.includes("{name}")) {
    return { before: template, link: null, after: "" };
  }
  const idx = template.indexOf("{name}");
  const before = template.slice(0, idx);
  const after = template.slice(idx + "{name}".length);
  const displayName = ctx.entityName ?? "—";
  const target = entityTarget(ctx.type);
  if (ctx.entityId === null || target === null) {
    return { before: before + displayName + after, link: null, after: "" };
  }
  return { before, link: { text: displayName, href: `${target}/${ctx.entityId}` }, after };
}

function pickTemplate(ctx: MessageContext): string {
  const t = ctx.templates;
  switch (ctx.type) {
    case "COMPANY_CREATED": return t.company.created;
    case "COMPANY_UPDATED": return t.company.updated;
    case "COMPANY_DELETED": return t.company.deleted;
    case "CONTACT_CREATED": return t.contact.created;
    case "CONTACT_UPDATED": return t.contact.updated;
    case "CONTACT_DELETED": return t.contact.deleted;
    case "COMPANY_COMMENT_CREATED": return t.companyComment.created;
    case "COMPANY_COMMENT_UPDATED": return t.companyComment.updated;
    case "COMPANY_COMMENT_DELETED": return t.companyComment.deleted;
    case "CONTACT_COMMENT_CREATED": return t.contactComment.created;
    case "CONTACT_COMMENT_UPDATED": return t.contactComment.updated;
    case "CONTACT_COMMENT_DELETED": return t.contactComment.deleted;
  }
}

function entityTarget(type: UpdateType): "/companies" | "/contacts" | null {
  if (type.startsWith("COMPANY_DELETED") || type.startsWith("CONTACT_DELETED")) {
    return null;
  }
  return type.startsWith("COMPANY_") ? "/companies" : "/contacts";
}

function formatBy(template: string, userName: string): string {
  return template.replace("{user}", userName);
}

export function UpdatesClient() {
  const t = useTranslations();
  const [entries, setEntries] = useState<readonly UpdateEntryDto[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());

  const fetchUpdates = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await getUpdates({ size: pageSize });
      setEntries(result.content);
    } catch (err: unknown) {
      console.error("Failed to load updates", err);
      setError(t.updates.loadError);
      setEntries(null);
    } finally {
      setLoading(false);
    }
  }, [pageSize, t.updates.loadError]);

  useEffect(() => {
    fetchUpdates();
  }, [fetchUpdates]);

  const handlePageSizeChange = (raw: string) => {
    const next = Number(raw);
    if (!(PAGE_SIZE_OPTIONS as readonly number[]).includes(next)) return;
    if (typeof window !== "undefined") {
      window.localStorage.setItem(PAGE_SIZE_STORAGE_KEY, String(next));
    }
    setPageSize(next);
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between gap-4">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{t.updates.title}</h1>
        <div className="flex items-center gap-2">
          <span className="text-sm text-oe-gray">{t.updates.perPage}</span>
          <Select value={String(pageSize)} onValueChange={handlePageSizeChange}>
            <SelectTrigger
              className="w-[100px] h-8 text-sm"
              aria-label={t.updates.perPage}
              data-testid="updates-page-size"
            >
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {PAGE_SIZE_OPTIONS.map((option) => (
                <SelectItem key={option} value={String(option)}>
                  {option}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      </div>

      {loading ? (
        <div className="space-y-3" data-testid="updates-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : error ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="updates-error"
          role="alert"
        >
          <AlertCircle className="mb-4 h-12 w-12 text-oe-red/70" />
          <p className="text-oe-red">{error}</p>
        </div>
      ) : !entries || entries.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="updates-empty"
        >
          <Bell className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="text-oe-gray">{t.updates.empty}</p>
        </div>
      ) : (
        <ul className="divide-y divide-oe-gray-light rounded-lg border border-oe-gray-light bg-white">
          {entries.map((entry) => {
            const rendered = renderTemplate({
              type: entry.type,
              entityId: entry.entityId,
              entityName: entry.entityName,
              templates: t.updates.events,
            });
            return (
              <li
                key={entry.id}
                className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-center sm:justify-between"
                data-testid="updates-row"
              >
                <span className="text-oe-dark">
                  {rendered.before}
                  {rendered.link ? (
                    <Link
                      href={rendered.link.href}
                      className="font-medium text-oe-blue underline-offset-2 hover:underline"
                    >
                      {rendered.link.text}
                    </Link>
                  ) : null}
                  {rendered.after}
                </span>
                <span className="text-sm text-oe-gray">
                  {formatBy(t.updates.by, entry.user.name)} ·{" "}
                  {new Date(entry.createdAt).toLocaleString()}
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
