"use client";

import { useCallback, useEffect, useState } from "react";
import { useSession } from "next-auth/react";
import { MessageSquarePlus, X } from "lucide-react";
import { Button, DeleteConfirmDialog, Tooltip, TooltipTrigger, TooltipContent, Card, CardContent, CardHeader, CardTitle, Skeleton, MarkdownView } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { AddCommentDialog } from "@/components/add-comment-dialog";
import { TranslateButton } from "@/components/translate-button";
import { getContactComments, createContactComment, deleteContactComment, ForbiddenError } from "@/lib/api";
import type { CommentDto } from "@/lib/types";
import { hasRole, ROLE_ADMIN } from "@/lib/roles";

function formatDate(dateString: string, language: string): string {
  const date = new Date(dateString);
  return date.toLocaleString(language === "de" ? "de-DE" : "en-US", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface ContactCommentsProps {
  readonly contactId: string;
  readonly totalCount?: number;
}

export function ContactComments({ contactId, totalCount }: ContactCommentsProps) {
  const t = useTranslations();
  const S = t.companies.comments;
  const { data: session } = useSession();
  const canDelete = hasRole(session, ROLE_ADMIN);

  const [displayCount, setDisplayCount] = useState(totalCount);
  const [comments, setComments] = useState<CommentDto[]>([]);
  const [loading, setLoading] = useState(true);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const fetchComments = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getContactComments(contactId);
      setComments(result);
    } catch {
      setComments([]);
    } finally {
      setLoading(false);
    }
  }, [contactId]);

  useEffect(() => {
    setDisplayCount(totalCount);
  }, [totalCount]);

  useEffect(() => {
    fetchComments();
  }, [fetchComments]);

  const handleSend = async (text: string) => {
    setSending(true);
    try {
      const newComment = await createContactComment(contactId, { text });
      setComments((prev) => [newComment, ...prev]);
      setDisplayCount((prev) => (prev !== undefined ? prev + 1 : undefined));
    } finally {
      setSending(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteContactComment(contactId, deleteTarget);
      setComments((prev) => prev.filter((c) => c.id !== deleteTarget));
      setDisplayCount((prev) => (prev !== undefined && prev > 0 ? prev - 1 : prev));
      setDeleteTarget(null);
      setDeleteError(null);
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        setDeleteError(S.deleteDialog.title);
      }
    }
  };

  return (
    <Card className="border-oe-gray-light">
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle className="font-heading text-lg text-oe-dark">
          {S.title}{displayCount !== undefined ? ` (${displayCount})` : ""}
        </CardTitle>
        <Button
          onClick={() => setDialogOpen(true)}
          size="sm"
        >
          <MessageSquarePlus className="mr-2 h-4 w-4" />
          {S.add}
        </Button>
      </CardHeader>
      <CardContent>
        {loading && (
          <div className="space-y-4">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="space-y-2">
                <Skeleton className="h-4 w-1/3" />
                <Skeleton className="h-4 w-full" />
              </div>
            ))}
          </div>
        )}

        {!loading && comments.length === 0 && (
          <p className="text-sm text-oe-gray-mid">{S.empty}</p>
        )}

        {!loading && comments.length > 0 && (
          <div className="space-y-4">
            {comments.map((comment) => (
              <div key={comment.id} className="border-b border-oe-gray-light pb-4 last:border-b-0">
                <div className="flex items-start justify-between">
                  <p className="text-xs text-oe-gray-mid">
                    {comment.author?.name ?? "—"} &middot; {formatDate(comment.createdAt, "de")}
                  </p>
                  <div className="flex items-center">
                    <TranslateButton text={comment.text} size="md" />
                    <Tooltip>
                      <TooltipTrigger asChild>
                        <span>
                          <button
                            type="button"
                            disabled={!canDelete}
                            onClick={() => { setDeleteTarget(comment.id); setDeleteError(null); }}
                            className="text-oe-red hover:text-oe-red-dark shrink-0 ml-2 disabled:opacity-40 disabled:pointer-events-auto disabled:cursor-not-allowed"
                          >
                            <X className="h-4 w-4" />
                          </button>
                        </span>
                      </TooltipTrigger>
                      <TooltipContent>{canDelete ? S.deleteDialog.title : t.errors.roleRequired.admin}</TooltipContent>
                    </Tooltip>
                  </div>
                </div>
                <div className="mt-1 text-sm">
                  <MarkdownView content={comment.text} />
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      <AddCommentDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSubmit={handleSend}
        sending={sending}
        title={S.addTitle}
        placeholder={S.placeholder}
        sendLabel={S.send}
        sendingLabel={S.sending}
        errorTitle={S.errorTitle}
        errorMessage={S.errorGeneric}
      />

      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) { setDeleteTarget(null); setDeleteError(null); } }}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </Card>
  );
}
