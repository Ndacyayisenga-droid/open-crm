"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Plus, Radio, Trash2, Webhook } from "lucide-react";
import {
  Button,
  DeleteConfirmDialog,
  Input,
  Tooltip,
  TooltipTrigger,
  TooltipContent,
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TablePagination,
  TooltipIconButton,
} from "@open-elements/ui";
import { useAppLayerTranslations } from "../../translations/provider";
import { useApiClient } from "../../hooks/api-client";
import type { WebhookDto, Page } from "../../api/types";
import type { AppLayerTranslations } from "../../translations/provider";

export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "pageSize.webhooks";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  return DEFAULT_PAGE_SIZE;
}

function formatStatus(status: number | null, t: AppLayerTranslations): string {
  if (status === null) return t.webhooks.status.neverCalled;
  if (status === -1) return t.webhooks.status.timeout;
  if (status === 0) return t.webhooks.status.connectionError;
  if (status >= 200 && status < 300) return t.webhooks.status.ok;
  return `${t.webhooks.status.badCall} (${status})`;
}

function formatTimestamp(ts: string | null): string {
  if (!ts) return "—";
  return new Date(ts).toLocaleString();
}

export function WebhooksClient() {
  const t = useAppLayerTranslations();
  const api = useApiClient();
  const [data, setData] = useState<Page<WebhookDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());
  const [deleteTarget, setDeleteTarget] = useState<WebhookDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createUrl, setCreateUrl] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);

  const fetchWebhooks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getWebhooks({ page, size: pageSize });
      setData(result);
    } catch (err: unknown) {
      console.error("Failed to load webhooks", err);
      setError(t.webhooks.loadError);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [api, page, pageSize, t.webhooks.loadError]);

  useEffect(() => {
    fetchWebhooks();
  }, [fetchWebhooks]);

  const handleCreate = async () => {
    if (!createUrl.trim()) {
      setCreateError(t.webhooks.createDialog.urlRequired);
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      await api.createWebhook({ url: createUrl.trim() });
      setCreateOpen(false);
      setCreateUrl("");
      setCreateError(null);
      fetchWebhooks();
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : t.webhooks.createDialog.error);
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await api.deleteWebhook(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchWebhooks();
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "Unknown error");
    }
  };

  const handleToggleActive = async (webhook: WebhookDto) => {
    try {
      await api.updateWebhook(webhook.id, {
        url: webhook.url,
        active: !webhook.active,
      });
      fetchWebhooks();
    } catch {
      // Silently fail — user can retry
    }
  };

  const handlePing = async (webhook: WebhookDto) => {
    try {
      await api.pingWebhook(webhook.id);
    } catch {
      // Fire-and-forget — no visible feedback
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{t.webhooks.title}</h1>
        <Button
          onClick={() => {
            setCreateUrl("");
            setCreateError(null);
            setCreateOpen(true);
          }}
        >
          <Plus className="mr-2 h-4 w-4" />
          {t.webhooks.newWebhook}
        </Button>
      </div>

      {loading ? (
        <div className="space-y-3" data-testid="webhooks-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : error ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="webhooks-error"
          role="alert"
        >
          <AlertCircle className="mb-4 h-12 w-12 text-oe-red/70" />
          <p className="text-oe-red">{error}</p>
        </div>
      ) : !data || data.content.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="webhooks-empty"
        >
          <Webhook className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="mb-4 text-oe-gray">{t.webhooks.empty}</p>
          <Button
            onClick={() => {
              setCreateUrl("");
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            {t.webhooks.createFirst}
          </Button>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.webhooks.columns.url}</TableHead>
                  <TableHead className="w-20 text-center">{t.webhooks.columns.active}</TableHead>
                  <TableHead className="hidden md:table-cell">{t.webhooks.columns.lastStatus}</TableHead>
                  <TableHead className="hidden md:table-cell">{t.webhooks.columns.lastCalledAt}</TableHead>
                  <TableHead className="w-24 text-right">{t.webhooks.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((webhook) => (
                  <TableRow key={webhook.id}>
                    <TableCell className="max-w-xs truncate font-medium text-oe-dark">
                      {webhook.url}
                    </TableCell>
                    <TableCell className="text-center">
                      <span
                        className={`inline-block h-3 w-3 rounded-full ${webhook.active ? "bg-oe-green" : "bg-oe-gray"}`}
                      />
                    </TableCell>
                    <TableCell className="hidden text-oe-gray md:table-cell">
                      {formatStatus(webhook.lastStatus, t)}
                    </TableCell>
                    <TableCell className="hidden text-oe-gray md:table-cell">
                      {formatTimestamp(webhook.lastCalledAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-oe-gray hover:text-oe-dark"
                              onClick={() => handleToggleActive(webhook)}
                            >
                              {webhook.active ? (
                                <span className="text-xs font-bold">OFF</span>
                              ) : (
                                <span className="text-xs font-bold">ON</span>
                              )}
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>
                            {webhook.active ? t.webhooks.actions.deactivate : t.webhooks.actions.activate}
                          </TooltipContent>
                        </Tooltip>
                        <TooltipIconButton
                          icon={<Radio />}
                          tooltip={t.webhooks.actions.ping}
                          onClick={() => handlePing(webhook)}
                        />
                        <TooltipIconButton
                          icon={<Trash2 />}
                          tone="destructive"
                          tooltip={t.webhooks.actions.delete}
                          onClick={() => {
                            setDeleteTarget(webhook);
                            setDeleteError(null);
                          }}
                        />
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <TablePagination
            page={page}
            pageSize={pageSize}
            totalElements={totalElements}
            totalPages={totalPages}
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            storageKey={PAGE_SIZE_STORAGE_KEY}
            translations={t.webhooks.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.webhooks.createDialog.title}</DialogTitle>
            <DialogDescription>{t.webhooks.createDialog.urlLabel}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Input
              placeholder={t.webhooks.createDialog.urlPlaceholder}
              value={createUrl}
              onChange={(e) => {
                setCreateUrl(e.target.value);
                setCreateError(null);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleCreate();
              }}
            />
            {createError && <p className="text-sm text-oe-red">{createError}</p>}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)}>
              {t.webhooks.createDialog.cancel}
            </Button>
            <Button onClick={handleCreate} disabled={createSubmitting}>
              {t.webhooks.createDialog.create}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete dialog */}
      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        onConfirm={handleDelete}
        title={t.webhooks.deleteDialog.title}
        description={t.webhooks.deleteDialog.description}
        confirmLabel={t.webhooks.deleteDialog.confirm}
        cancelLabel={t.webhooks.deleteDialog.cancel}
        error={deleteError}
        errorTitle={t.webhooks.deleteDialog.errorTitle}
      />
    </div>
  );
}
