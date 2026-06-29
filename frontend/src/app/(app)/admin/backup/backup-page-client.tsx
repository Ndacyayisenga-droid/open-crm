"use client";

import { useCallback, useEffect, useState } from "react";
import { Copy, Download, Loader2 } from "lucide-react";
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import {
  backupDownloadUrl,
  getBackups,
  getBackupStatus,
  triggerBackup,
} from "@/lib/api";
import type {
  BackupItemDto,
  BackupStatusDto,
  BackupTriggerDto,
} from "@/lib/types";

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  const mb = kb / 1024;
  if (mb < 1024) return `${mb.toFixed(1)} MB`;
  const gb = mb / 1024;
  return `${gb.toFixed(2)} GB`;
}

function formatRelativeAge(seconds: number): string {
  if (seconds < 60) return `${seconds}s`;
  const min = Math.floor(seconds / 60);
  if (min < 60) return `${min}min`;
  const hours = Math.floor(min / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  return `${days}d`;
}

function formatDurationMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`;
  const s = ms / 1000;
  return `${s.toFixed(1)}s`;
}

function StatusCard({
  status,
  loading,
  loadError,
  translations,
}: {
  status: BackupStatusDto | null;
  loading: boolean;
  loadError: boolean;
  translations: ReturnType<typeof useTranslations>["backup"]["status"];
}) {
  const S = translations;
  let body: React.ReactNode;
  if (loading) {
    body = (
      <div className="flex items-center gap-2 text-oe-gray-dark">
        <Loader2 className="h-4 w-4 animate-spin" />
      </div>
    );
  } else if (loadError || !status) {
    body = <p className="text-oe-red">{S.unavailable}</p>;
  } else if (!status.configured) {
    body = <p className="text-oe-gray-dark">{S.notConfigured}</p>;
  } else if (!status.healthy) {
    body = (
      <div className="flex items-center gap-2">
        <span className="inline-block h-3 w-3 rounded-full bg-oe-red" aria-hidden="true" />
        <span>{S.unavailable}</span>
      </div>
    );
  } else {
    const info = status.info;
    body = (
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-3 w-3 rounded-full bg-oe-green"
            aria-hidden="true"
          />
          <span>{S.healthy}</span>
        </div>
        {info === null ? (
          <p className="text-oe-gray-dark text-sm">{S.infoUnavailable}</p>
        ) : (
          <dl className="grid grid-cols-1 gap-1 text-sm sm:grid-cols-2">
            <div>
              <dt className="font-medium">{S.version}</dt>
              <dd>{info.version}</dd>
            </div>
            <div>
              <dt className="font-medium">{S.pgDumpVersion}</dt>
              <dd>{info.pgDumpVersion}</dd>
            </div>
            <div>
              <dt className="font-medium">{S.retention}</dt>
              <dd>{info.retention.days}d</dd>
            </div>
            <div>
              <dt className="font-medium">{S.interval}</dt>
              <dd>{info.backupInterval.iso8601}</dd>
            </div>
            <div>
              <dt className="font-medium">{S.lastBackupAge}</dt>
              <dd>{formatRelativeAge(info.backup.lastSuccessfulBackupAgeSeconds)}</dd>
            </div>
          </dl>
        )}
      </div>
    );
  }
  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{S.title}</CardTitle>
      </CardHeader>
      <CardContent>{body}</CardContent>
    </Card>
  );
}

function TriggerCard({
  translations,
}: {
  translations: ReturnType<typeof useTranslations>["backup"]["trigger"];
}) {
  const S = translations;
  const [pending, setPending] = useState(false);
  const [result, setResult] = useState<BackupTriggerDto | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleClick = async () => {
    setPending(true);
    setError(null);
    setResult(null);
    try {
      const r = await triggerBackup();
      setResult(r);
    } catch {
      setError(S.errorGeneric);
    } finally {
      setPending(false);
    }
  };

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{S.title}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <Button onClick={handleClick} disabled={pending}>
          {pending ? <Loader2 className="h-4 w-4 animate-spin" /> : S.button}
        </Button>
        {result !== null && (
          <p>
            {(result.alreadyRunning ? S.alreadyRunning : S.triggered).replace(
              "{jobId}",
              result.jobId,
            )}
          </p>
        )}
        {error !== null && <p className="text-oe-red">{error}</p>}
      </CardContent>
    </Card>
  );
}

function Sha256Cell({
  value,
  copyLabel,
  copiedLabel,
}: {
  value: string;
  copyLabel: string;
  copiedLabel: string;
}) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard access can fail in non-secure contexts; nothing to surface here.
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      className="inline-flex max-w-[18rem] items-center gap-1 truncate rounded bg-oe-gray-light/40 px-2 py-1 font-mono text-xs hover:bg-oe-gray-light"
      title={copied ? copiedLabel : copyLabel}
    >
      <Copy className="h-3 w-3 shrink-0" />
      <span className="truncate">{value}</span>
    </button>
  );
}

function BackupsCard({
  translations,
}: {
  translations: ReturnType<typeof useTranslations>["backup"]["list"];
}) {
  const S = translations;
  const [items, setItems] = useState<BackupItemDto[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await getBackups());
    } catch {
      setError(S.errorGeneric);
    } finally {
      setLoading(false);
    }
  }, [S.errorGeneric]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  let body: React.ReactNode;
  if (loading) {
    body = (
      <div className="flex items-center gap-2 text-oe-gray-dark">
        <Loader2 className="h-4 w-4 animate-spin" />
      </div>
    );
  } else if (error !== null) {
    body = <p className="text-oe-red">{error}</p>;
  } else if (!items || items.length === 0) {
    body = <p className="text-oe-gray-dark">{S.empty}</p>;
  } else {
    body = (
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>{S.columns.createdAt}</TableHead>
            <TableHead>{S.columns.sizeBytes}</TableHead>
            <TableHead>{S.columns.pgVersion}</TableHead>
            <TableHead>{S.columns.durationMs}</TableHead>
            <TableHead>{S.columns.triggeredBy}</TableHead>
            <TableHead>{S.columns.sha256}</TableHead>
            <TableHead>{S.columns.action}</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {items.map((item) => (
            <TableRow key={item.id}>
              <TableCell>{new Date(item.createdAt).toLocaleString()}</TableCell>
              <TableCell>{formatBytes(item.sizeBytes)}</TableCell>
              <TableCell>{item.pgVersion}</TableCell>
              <TableCell>{formatDurationMs(item.durationMs)}</TableCell>
              <TableCell>
                {item.triggeredBy ? <Badge>{item.triggeredBy}</Badge> : null}
              </TableCell>
              <TableCell>
                <Sha256Cell
                  value={item.sha256}
                  copyLabel={S.sha256Copy}
                  copiedLabel={S.sha256Copied}
                />
              </TableCell>
              <TableCell>
                <Button asChild variant="outline" size="sm">
                  <a href={backupDownloadUrl(item.id)}>
                    <Download className="mr-1 h-4 w-4" />
                    {S.download}
                  </a>
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    );
  }

  return (
    <Card className="border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-lg text-oe-dark">{S.title}</CardTitle>
      </CardHeader>
      <CardContent>{body}</CardContent>
    </Card>
  );
}

export function BackupPageClient() {
  const t = useTranslations();
  const [status, setStatus] = useState<BackupStatusDto | null>(null);
  const [statusLoading, setStatusLoading] = useState(true);
  const [statusError, setStatusError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setStatusLoading(true);
    setStatusError(false);
    getBackupStatus()
      .then((s) => {
        if (!cancelled) setStatus(s);
      })
      .catch(() => {
        if (!cancelled) setStatusError(true);
      })
      .finally(() => {
        if (!cancelled) setStatusLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">{t.backup.title}</h1>
      <div className="space-y-4">
        <StatusCard
          status={status}
          loading={statusLoading}
          loadError={statusError}
          translations={t.backup.status}
        />
        <TriggerCard translations={t.backup.trigger} />
        <BackupsCard translations={t.backup.list} />
      </div>
    </div>
  );
}
