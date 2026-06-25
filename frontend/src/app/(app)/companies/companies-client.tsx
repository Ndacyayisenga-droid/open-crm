"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Plus, Trash2, Building2, Printer, Pencil, MessageSquarePlus, FileDown, ExternalLink } from "lucide-react";
import { Button, Input, TagMultiSelect, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, Table, TableBody, TableCell, TableHead, TableHeader, TableRow, Skeleton } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { ActionIconButton, CopyToClipboardButton, ExternalLinkButton, TablePagination, TooltipIconButton } from "@open-elements/ui";
import { AddCommentDialog } from "@open-elements/nextjs-app-layer";
import { CompanyDeleteDialog } from "@/components/company-delete-dialog";
import { CsvExportDialog } from "@/components/csv-export-dialog";
import { getCompanies, deleteCompany, getCompanyLogoUrl, createCompanyComment, getCompanyExportUrl, getTags, ForbiddenError } from "@/lib/api";
import type { CompanyDto, Page } from "@/lib/types";
import { hasAppAdmin } from "@/lib/roles";

function WebsiteCell({ value }: { readonly value: string | null }) {
  if (!value) return <TableCell className="text-oe-gray-mid">—</TableCell>;
  const href = value.startsWith("http") ? value : `https://${value}`;
  return (
    <TableCell className="text-oe-gray-mid">
      <span className="inline-flex items-center gap-1">
        <span>{value}</span>
        <span className="inline-flex gap-0.5 shrink-0">
          <CopyToClipboardButton value={value} />
          <ExternalLinkButton href={href} />
        </span>
      </span>
    </TableCell>
  );
}

function ContactCountCell({ count, companyId }: { readonly count: number; readonly companyId: string }) {
  const router = useRouter();
  return (
    <TableCell>
      <span className="inline-flex items-center gap-1">
        <span>{count}</span>
        {count > 0 && (
          <ActionIconButton onClick={() => router.push(`/contacts?companyId=${companyId}`)}>
            <ExternalLink />
          </ActionIconButton>
        )}
      </span>
    </TableCell>
  );
}

export function CompaniesClient() {
  const t = useTranslations();
  const S = t.companies;
  const router = useRouter();
  const searchParams = useSearchParams();
  const { data: session } = useSession();
  const canDelete = hasAppAdmin(session);
  const [data, setData] = useState<Page<CompanyDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(() => {
    if (typeof window === "undefined") return 20;
    const stored = localStorage.getItem("pageSize.companies");
    const parsed = Number(stored);
    if ([10, 20, 50, 100, 200].includes(parsed)) return parsed;
    localStorage.setItem("pageSize.companies", "20");
    return 20;
  });
  const [nameFilter, setNameFilter] = useState("");
  const [brevoFilter, setBrevoFilter] = useState("all");
  const [tagIds, setTagIds] = useState<string[]>(() => {
    const param = searchParams.get("tagIds");
    return param ? param.split(",") : [];
  });

  const [deleteTarget, setDeleteTarget] = useState<CompanyDto | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [commentTarget, setCommentTarget] = useState<CompanyDto | null>(null);
  const [commentSending, setCommentSending] = useState(false);
  const [csvOpen, setCsvOpen] = useState(false);

  const companyColumns = Object.entries(t.csvExport.companyColumns).map(([key, label]) => ({
    key: key.replace(/([A-Z])/g, "_$1").toUpperCase(),
    label,
  }));

  const fetchCompanies = useCallback(async () => {
    setLoading(true);
    try {
      const result = await getCompanies({
        page,
        size: pageSize,
        name: nameFilter || undefined,
        brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
        tagIds: tagIds.length > 0 ? tagIds : undefined,
      });
      setData(result);
    } catch {
      console.error("Failed to fetch companies");
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, nameFilter, brevoFilter, tagIds]);

  useEffect(() => {
    fetchCompanies();
  }, [fetchCompanies]);

  useEffect(() => {
    setPage(0);
  }, [nameFilter, brevoFilter, tagIds]);

  const handleDeleteAll = async () => {
    if (!deleteTarget) return;
    try {
      await deleteCompany(deleteTarget.id, true);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchCompanies();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        console.error("Failed to delete company");
      }
    }
  };

  const handleDeleteCompanyOnly = async () => {
    if (!deleteTarget) return;
    try {
      await deleteCompany(deleteTarget.id, false);
      setDeleteTarget(null);
      setDeleteError(null);
      fetchCompanies();
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        console.error("Failed to delete company");
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
              if (nameFilter) params.set("name", nameFilter);
              if (brevoFilter !== "all") params.set("brevo", brevoFilter);
              if (tagIds.length > 0) params.set("tagIds", tagIds.join(","));
              const query = params.toString();
              window.open(`/companies/print${query ? `?${query}` : ""}`, "_blank");
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
          <Button asChild>
            <Link href="/companies/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newCompany}
            </Link>
          </Button>
        </div>
      </div>

      {/* Filters */}
      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <Input
          placeholder={S.filter.name}
          value={nameFilter}
          onChange={(e) => setNameFilter(e.target.value)}
          className="sm:max-w-[200px]"
        />
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
              router.replace(`/companies${query ? `?${query}` : ""}`, { scroll: false });
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
            <Link href="/companies/new">
              <Plus className="mr-2 h-4 w-4" />
              {S.newCompany}
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
                  <TableHead>{S.columns.website}</TableHead>
                  <TableHead>{S.columns.contacts}</TableHead>
                  <TableHead className="w-[140px] text-right">{S.columns.actions}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((company) => (
                  <TableRow
                    key={company.id}
                    className="cursor-pointer"
                    onClick={() => router.push(`/companies/${company.id}`)}
                  >
                    <TableCell>
                      {company.hasLogo ? (
                        <img
                          src={getCompanyLogoUrl(company.id)}
                          alt={company.name}
                          className="h-8 w-8 object-contain"
                        />
                      ) : (
                        <Building2 className="h-8 w-8 text-oe-gray-mid" />
                      )}
                    </TableCell>
                    <TableCell className="font-medium">{company.name}</TableCell>
                    <WebsiteCell value={company.website} />
                    <ContactCountCell count={company.contactCount} companyId={company.id} />
                    <TableCell className="text-right">
                      <TooltipIconButton
                        icon={<Pencil />}
                        tooltip={S.detail.edit}
                        onClick={() => router.push(`/companies/${company.id}/edit`)}
                      />
                      <TooltipIconButton
                        icon={<MessageSquarePlus />}
                        tooltip={S.comments.addTitle}
                        onClick={() => setCommentTarget(company)}
                      />
                      <TooltipIconButton
                        icon={<Trash2 />}
                        tone="destructive"
                        tooltip={canDelete ? S.detail.delete : t.errors.roleRequired.admin}
                        disabled={!canDelete}
                        onClick={() => {
                          setDeleteError(null);
                          setDeleteTarget(company);
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
            storageKey="pageSize.companies"
            translations={S.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}

      {/* Delete dialog */}
      <CompanyDeleteDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => {
          if (!open) {
            setDeleteTarget(null);
            setDeleteError(null);
          }
        }}
        companyName={deleteTarget?.name ?? ""}
        onDeleteAll={handleDeleteAll}
        onDeleteCompanyOnly={handleDeleteCompanyOnly}
        title={S.deleteDialog.title}
        descriptionAll={S.deleteDialog.descriptionAll}
        descriptionOnly={S.deleteDialog.descriptionOnly}
        deleteAllLabel={S.deleteDialog.deleteAll}
        deleteOnlyLabel={S.deleteDialog.deleteOnly}
        cancelLabel={S.deleteDialog.cancel}
        error={deleteError}
      />

      <AddCommentDialog
        open={commentTarget !== null}
        onOpenChange={(open) => { if (!open) setCommentTarget(null); }}
        onSubmit={async (text) => {
          setCommentSending(true);
          try {
            await createCompanyComment(commentTarget!.id, { text });
            setCommentTarget(null);
          } finally {
            setCommentSending(false);
          }
        }}
        sending={commentSending}
        title={S.comments.addTitle}
        placeholder={S.comments.placeholder}
        sendLabel={S.comments.send}
        sendingLabel={S.comments.sending}
        errorTitle={S.comments.errorTitle}
        errorMessage={S.comments.errorGeneric}
      />

      <CsvExportDialog
        open={csvOpen}
        onOpenChange={setCsvOpen}
        columns={companyColumns}
        onDownload={(columns) => {
          const url = getCompanyExportUrl(
            {
              name: nameFilter || undefined,
              brevo: brevoFilter === "all" ? undefined : brevoFilter === "true",
              tagIds: tagIds.length > 0 ? tagIds : undefined,
            },
            columns,
          );
          window.open(url, "_blank");
        }}
      />
    </div>
  );
}
