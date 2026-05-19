"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, Check, Copy, KeyRound, Plus, Trash2 } from "lucide-react";
import {
  Button,
  DeleteConfirmDialog,
  Input,
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
import type { ApiKeyDto, ApiKeyCreatedDto, Page } from "../../api/types";

export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "pageSize.apiKeys";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  return DEFAULT_PAGE_SIZE;
}

export function ApiKeysClient() {
  const t = useAppLayerTranslations();
  const api = useApiClient();
  const [data, setData] = useState<Page<ApiKeyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());
  const [deleteTarget, setDeleteTarget] = useState<ApiKeyDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createError, setCreateError] = useState<string | null>(null);
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createdKey, setCreatedKey] = useState<ApiKeyCreatedDto | null>(null);
  const [copied, setCopied] = useState(false);

  const fetchApiKeys = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getApiKeys({ page, size: pageSize });
      setData(result);
    } catch (err: unknown) {
      console.error("Failed to load API keys", err);
      setError(t.apiKeys.loadError);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [api, page, pageSize, t.apiKeys.loadError]);

  useEffect(() => {
    fetchApiKeys();
  }, [fetchApiKeys]);

  const handleCreate = async () => {
    if (!createName.trim()) {
      setCreateError(t.apiKeys.createDialog.nameRequired);
      return;
    }
    setCreateError(null);
    setCreateSubmitting(true);
    try {
      const result = await api.createApiKey({ name: createName.trim() });
      setCreateOpen(false);
      setCreateName("");
      setCreateError(null);
      setCreatedKey(result);
      setCopied(false);
    } catch (e) {
      setCreateError(e instanceof Error ? e.message : t.apiKeys.createDialog.error);
    } finally {
      setCreateSubmitting(false);
    }
  };

  const handleCopy = async () => {
    if (createdKey) {
      await navigator.clipboard.writeText(createdKey.key);
      setCopied(true);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await api.deleteApiKey(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchApiKeys();
    } catch (e) {
      setDeleteError(e instanceof Error ? e.message : "Unknown error");
    }
  };

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{t.apiKeys.title}</h1>
        <Button
          onClick={() => {
            setCreateName("");
            setCreateError(null);
            setCreateOpen(true);
          }}
        >
          <Plus className="mr-2 h-4 w-4" />
          {t.apiKeys.newApiKey}
        </Button>
      </div>

      {loading ? (
        <div className="space-y-3" data-testid="api-keys-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : error ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="api-keys-error"
          role="alert"
        >
          <AlertCircle className="mb-4 h-12 w-12 text-oe-red/70" />
          <p className="text-oe-red">{error}</p>
        </div>
      ) : !data || data.content.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="api-keys-empty"
        >
          <KeyRound className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="mb-4 text-oe-gray">{t.apiKeys.empty}</p>
          <Button
            onClick={() => {
              setCreateName("");
              setCreateError(null);
              setCreateOpen(true);
            }}
          >
            {t.apiKeys.createFirst}
          </Button>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t.apiKeys.columns.name}</TableHead>
                  <TableHead>{t.apiKeys.columns.key}</TableHead>
                  <TableHead className="hidden md:table-cell">{t.apiKeys.columns.createdBy}</TableHead>
                  <TableHead className="hidden md:table-cell">{t.apiKeys.columns.createdAt}</TableHead>
                  <TableHead className="w-20 text-right">{t.apiKeys.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((apiKey) => (
                  <TableRow key={apiKey.id}>
                    <TableCell className="font-medium text-oe-dark">{apiKey.name}</TableCell>
                    <TableCell className="font-mono text-sm text-oe-gray">{apiKey.keyPrefix}</TableCell>
                    <TableCell className="hidden text-oe-gray md:table-cell">{apiKey.createdBy}</TableCell>
                    <TableCell className="hidden text-oe-gray md:table-cell">
                      {new Date(apiKey.createdAt).toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      <TooltipIconButton
                        icon={<Trash2 />}
                        tone="destructive"
                        tooltip={t.apiKeys.actions.delete}
                        onClick={() => {
                          setDeleteTarget(apiKey);
                          setDeleteError(null);
                        }}
                      />
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
            translations={t.apiKeys.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.apiKeys.createDialog.title}</DialogTitle>
            <DialogDescription>{t.apiKeys.createDialog.nameLabel}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <Input
              placeholder={t.apiKeys.createDialog.namePlaceholder}
              value={createName}
              onChange={(e) => {
                setCreateName(e.target.value);
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
              {t.apiKeys.createDialog.cancel}
            </Button>
            <Button onClick={handleCreate} disabled={createSubmitting}>
              {t.apiKeys.createDialog.create}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Key created dialog */}
      <Dialog
        open={createdKey !== null}
        onOpenChange={(open) => {
          if (!open) {
            setCreatedKey(null);
            fetchApiKeys();
          }
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t.apiKeys.keyDialog.title}</DialogTitle>
            <DialogDescription>{t.apiKeys.keyDialog.warning}</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div className="rounded-lg border border-oe-gray-light bg-oe-gray-lightest p-4">
              <code className="block break-all font-mono text-sm text-oe-dark">
                {createdKey?.key}
              </code>
            </div>
            <Button variant="outline" className="w-full" onClick={handleCopy}>
              {copied ? (
                <>
                  <Check className="mr-2 h-4 w-4" />
                  {t.apiKeys.keyDialog.copied}
                </>
              ) : (
                <>
                  <Copy className="mr-2 h-4 w-4" />
                  {t.apiKeys.keyDialog.copy}
                </>
              )}
            </Button>
          </div>
          <DialogFooter>
            <Button
              onClick={() => {
                setCreatedKey(null);
                fetchApiKeys();
              }}
            >
              {t.apiKeys.keyDialog.close}
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
        title={t.apiKeys.deleteDialog.title}
        description={t.apiKeys.deleteDialog.description}
        confirmLabel={t.apiKeys.deleteDialog.confirm}
        cancelLabel={t.apiKeys.deleteDialog.cancel}
        error={deleteError}
        errorTitle={t.apiKeys.deleteDialog.errorTitle}
      />
    </div>
  );
}
