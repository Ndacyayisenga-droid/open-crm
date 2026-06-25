"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Plus, Trash2, User, Printer, Pencil, MessageSquarePlus, FileDown, FileUp, ExternalLink, Contact } from "lucide-react";
import { Button, DeleteConfirmDialog, Input, TagMultiSelect, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { ActionIconButton, CopyToClipboardButton, MailtoButton, TablePagination, TooltipIconButton } from "@open-elements/ui";
import { AddCommentDialog } from "@open-elements/nextjs-app-layer";
import { CsvExportDialog } from "@/components/csv-export-dialog";
import { CsvImportDialog } from "@/components/csv-import-dialog";
import { getContacts, deleteContact, getCompaniesForSelect, getContactPhotoUrl, createContactComment, getContactExportUrl, getContactVCardExportUrl, getTags, ForbiddenError } from "@/lib/api";
import type { ContactDto, CompanyDto, Page } from "@/lib/types";
import { hasAppAdmin, ROLE_APP_ADMIN, ROLE_IT_ADMIN } from "@/lib/roles";

function EmailCell({ value }: { readonly value: string | null }) {
  if (!value) return <TableCell className="text-oe-gray-mid">—</TableCell>;
  return (
    <TableCell className="text-oe-gray-mid">
      <span className="inline-flex items-center gap-1">
        <span>{value}</span>
        <span className="inline-flex gap-0.5 shrink-0">
          <CopyToClipboardButton value={value} />
          <MailtoButton email={value} />
        </span>
      </span>
    </TableCell>
  );
}

function CompanyNameCell({ name, companyId }: { readonly name: string | null; readonly companyId: string | null }) {
  const router = useRouter();
  if (!name || !companyId) return <TableCell className="text-oe-gray-mid">—</TableCell>;
  return (
    <TableCell className="text-oe-gray-mid">
      <span className="inline-flex items-center gap-1">
        <span>{name}</span>
        <span className="inline-flex gap-0.5 shrink-0">
          <CopyToClipboardButton value={name} />
          <ActionIconButton onClick={() => router.push(`/companies/${companyId}`)}>
            <ExternalLink />
          </ActionIconButton>
        </span>
      </span>
    </TableCell>
  );
}

export function ContactsClient() {
  const t = useTranslations();
  const S = t.contacts;
  const router = useRouter();
  const searchParams = useSearchParams();
  const { data: session } = useSession();
  const canDelete = hasAppAdmin(session);
  const canImport =
    session?.roles?.includes(ROLE_APP_ADMIN) || session?.roles?.includes(ROLE_IT_ADMIN);
  const [data, setData] = useState<Page<ContactDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.contacts");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.contacts", "20");
    return 20;
  });
  const [searchFilter, setSearchFilter] = useState("");
  const [companyIdFilter, setCompanyIdFilter] = useState(searchParams.get("companyId") ?? "");
  const [languageFilter, setLanguageFilter] = useState("");
  const [brevoFilter, setBrevoFilter] = useState("all");
  const [tagIds, setTagIds] = useState<string[]>(() => {
    const param = searchParams.get("tagIds");
    return param ? param.split(",") : [];
  });

  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [deleteTarget, setDeleteTarget] = useState<ContactDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [commentTarget, setCommentTarget] = useState<ContactDto | null>(null);
  const [commentSending, setCommentSending] = useState(false);
  const [csvOpen, setCsvOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  const contactColumns = Object.entries(t.csvExport.contactColumns).map(([key, label]) => ({
    key: key.replace(/([A-Z])/g, "_$1").toUpperCase(),
    label,
  }));

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
  }, []);

  const fetchContacts = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getContacts({
        page,
        size: pageSize,
        search: searchFilter || undefined,
        companyId: companyIdFilter && companyIdFilter !== "all" && companyIdFilter !== "none" ? companyIdFilter : undefined,
        noCompany: companyIdFilter === "none" ? true : undefined,
        language: languageFilter && languageFilter !== "all" ? languageFilter : undefined,
        brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch contacts");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, searchFilter, companyIdFilter, languageFilter, brevoFilter, tagIds]);

  useEffect(() => {
    fetchContacts();
  }, [fetchContacts]);

  useEffect(() => {
    setPage(0);
  }, [searchFilter, companyIdFilter, languageFilter, brevoFilter, tagIds]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteContact(deleteTarget.id);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchContacts();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        setDeleteError(S.form.errorGeneric);
      }
    }
  };

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <h1 className="font-heading text-2xl font-bold text-oe-dark">{S.title}</h1>
        <div className="flex gap-2">
          <Button
            variant="outline"
            onClick={() => {
              const params = new URLSearchParams();
              if (searchFilter) params.set("search", searchFilter);
              if (companyIdFilter === "none") {
                params.set("noCompany", "true");
              } else if (companyIdFilter && companyIdFilter !== "all") {
                params.set("companyId", companyIdFilter);
              }
              if (languageFilter && languageFilter !== "all") params.set("language", languageFilter);
              if (brevoFilter !== "all") params.set("brevo", brevoFilter);
              if (tagIds.length > 0) params.set("tagIds", tagIds.join(","));
              const query = params.toString();
              window.open(`/contacts/print${query ? `?${query}` : ""}`, "_blank");
            }}
          >
            <Printer className="mr-2 h-4 w-4" />
            {t.print.button}
          </Button>
          <Button
            variant="outline"
            disabled={!data || data.page.totalElements === 0}
            onClick={() => setCsvOpen(true)}
          >
            <FileDown className="mr-2 h-4 w-4" />
            {t.csvExport.button}
          </Button>
          {canImport && (
            <Button variant="outline" onClick={() => setImportOpen(true)}>
              <FileUp className="mr-2 h-4 w-4" />
              {t.csvImport.button}
            </Button>
          )}
          <Button
            variant="outline"
            disabled={!data || data.page.totalElements === 0}
            onClick={() => {
              const url = getContactVCardExportUrl({
                search: searchFilter || undefined,
                companyId: companyIdFilter && companyIdFilter !== "all" && companyIdFilter !== "none" ? companyIdFilter : undefined,
                noCompany: companyIdFilter === "none" ? true : undefined,
                language: languageFilter && languageFilter !== "all" ? languageFilter : undefined,
                brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
                tagIds: tagIds.length > 0 ? tagIds : undefined,
              });
              window.open(url, "_blank");
            }}
          >
            <Contact className="mr-2 h-4 w-4" />
            {t.vcardExport.button}
          </Button>
          <Button asChild>
            <Link href="/contacts/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newContact}
            </Link>
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:flex-wrap">
        <Input
          placeholder={S.filter.search}
          value={searchFilter}
          onChange={(e) => setSearchFilter(e.target.value)}
          className="sm:max-w-[180px]"
        />
        <Select value={companyIdFilter} onValueChange={setCompanyIdFilter}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={S.filter.company} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allCompanies}</SelectItem>
            <SelectItem value="none">{S.form.noCompany}</SelectItem>
            {companies.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={languageFilter} onValueChange={setLanguageFilter}>
          <SelectTrigger className="sm:max-w-[180px]">
            <SelectValue placeholder={S.filter.language} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{S.filter.allLanguages}</SelectItem>
            <SelectItem value="UNKNOWN">{S.filter.unknownLanguage}</SelectItem>
            <SelectItem value="DE">DE</SelectItem>
            <SelectItem value="EN">EN</SelectItem>
          </SelectContent>
        </Select>
        <Select value={brevoFilter} onValueChange={setBrevoFilter}>
          <SelectTrigger className="sm:max-w-[200px]">
            <SelectValue placeholder={t.brevoFilter.label} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t.brevoFilter.all}</SelectItem>
            <SelectItem value="true">{t.brevoFilter.fromBrevo}</SelectItem>
            <SelectItem value="false">{t.brevoFilter.notFromBrevo}</SelectItem>
          </SelectContent>
        </Select>
        <div className="sm:max-w-[250px]">
          <TagMultiSelect
            selectedIds={tagIds}
            onChange={(ids) => {
              setTagIds(ids);
              const params = new URLSearchParams(window.location.search);
              if (ids.length > 0) {
                params.set("tagIds", ids.join(","));
              } else {
                params.delete("tagIds");
              }
              const query = params.toString();
              router.replace(`/contacts${query ? `?${query}` : ""}`, { scroll: false });
            }}
            loadTags={async () => { const result = await getTags({ size: 1000 }); return result.content.map(tag => ({ value: tag.id, label: tag.name, color: tag.color })); }}
            translations={{ placeholder: t.tags.label + "...", empty: t.tags.empty }}
          />
        </div>
      </div>

      {/* Loading */}
      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && data && data.content.length === 0 && (
        <div className="flex flex-col items-center justify-center py-20 text-center">
          <p className="mb-4 text-oe-gray-mid">{S.empty}</p>
          <Button asChild>
            <Link href="/contacts/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newContact}
            </Link>
          </Button>
        </div>
      )}

      {/* Table */}
      {!loading && data && data.content.length > 0 && (
        <>
          <div className="rounded-md border border-oe-gray-light overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[50px]"></TableHead>
                  <TableHead>{S.columns.name}</TableHead>
                  <TableHead>{S.columns.email}</TableHead>
                  <TableHead>{S.columns.company}</TableHead>
                  <TableHead className="w-[140px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((contact) => (
                  <TableRow
                    key={contact.id}
                    className="cursor-pointer"
                    onClick={() => router.push(`/contacts/${contact.id}`)}
                  >
                    <TableCell>
                      {contact.hasPhoto ? (
                        <img
                          src={getContactPhotoUrl(contact.id)}
                          alt={`${contact.title ? contact.title + " " : ""}${contact.firstName} ${contact.lastName}`}
                          className="h-8 w-8 rounded-full object-cover"
                        />
                      ) : (
                        <User className="h-8 w-8 text-oe-gray-mid" />
                      )}
                    </TableCell>
                    <TableCell className="font-medium">
                      {`${contact.title ? contact.title + " " : ""}${contact.firstName} ${contact.lastName}`.trim()}
                    </TableCell>
                    <EmailCell value={contact.email} />
                    <CompanyNameCell name={contact.companyName} companyId={contact.companyId} />
                    <TableCell className="text-right">
                      <TooltipIconButton
                        icon={<Pencil />}
                        tooltip={S.detail.edit}
                        onClick={() => router.push(`/contacts/${contact.id}/edit`)}
                      />
                      <TooltipIconButton
                        icon={<MessageSquarePlus />}
                        tooltip={t.companies.comments.addTitle}
                        onClick={() => setCommentTarget(contact)}
                      />
                      <TooltipIconButton
                        icon={<Trash2 />}
                        tone="destructive"
                        tooltip={canDelete ? S.detail.delete : t.errors.roleRequired.admin}
                        disabled={!canDelete}
                        onClick={() => {
                          setDeleteError(null);
                          setDeleteTarget(contact);
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
            totalElements={data.page.totalElements}
            totalPages={data.page.totalPages}
            pageSizeOptions={[10, 20, 50, 100, 200]}
            storageKey="pageSize.contacts"
            translations={S.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Delete dialog */}
      <DeleteConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace(
          "{name}",
          deleteTarget ? `${deleteTarget.title ? deleteTarget.title + " " : ""}${deleteTarget.firstName} ${deleteTarget.lastName}` : "",
        )}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />

      <AddCommentDialog
        open={commentTarget !== null}
        onOpenChange={(open) => { if (!open) setCommentTarget(null); }}
        onSubmit={async (text) => {
          setCommentSending(true);
          try {
            await createContactComment(commentTarget!.id, { text });
            setCommentTarget(null);
          } finally {
            setCommentSending(false);
          }
        }}
        sending={commentSending}
        title={t.companies.comments.addTitle}
        placeholder={t.companies.comments.placeholder}
        sendLabel={t.companies.comments.send}
        sendingLabel={t.companies.comments.sending}
        errorTitle={t.companies.comments.errorTitle}
        errorMessage={t.companies.comments.errorGeneric}
      />

      <CsvExportDialog
        open={csvOpen}
        onOpenChange={setCsvOpen}
        columns={contactColumns}
        onDownload={(columns) => {
          const url = getContactExportUrl(
            {
              search: searchFilter || undefined,
              companyId: companyIdFilter && companyIdFilter !== "all" && companyIdFilter !== "none" ? companyIdFilter : undefined,
              noCompany: companyIdFilter === "none" ? true : undefined,
              language: languageFilter && languageFilter !== "all" ? languageFilter : undefined,
              brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
              tagIds: tagIds.length > 0 ? tagIds : undefined,
            },
            columns,
          );
          window.open(url, "_blank");
        }}
      />

      <CsvImportDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onComplete={fetchContacts}
      />
    </div>
  );
}
