"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Pencil, Trash2, Users, Building2 } from "lucide-react";
import { Button, Card, CardContent, CardHeader, CardTitle, DetailField, Separator, Tooltip, TooltipContent, TooltipTrigger, TagChips, MarkdownView, TranslateButton } from "@open-elements/ui";
import type { TagDto } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { CompanyDeleteDialog } from "@/components/company-delete-dialog";
import { CompanyComments } from "@/components/company-comments";
import { useTranslationConfig } from "@/lib/use-translation-config";
import { deleteCompany, ForbiddenError, getCompanyLogoUrl, getTag, translateText } from "@/lib/api";
import type { CompanyDto } from "@/lib/types";
import { hasRole, ROLE_ADMIN } from "@open-elements/nextjs-app-layer";

export function CompanyDetail({ company }: { readonly company: CompanyDto }) {
  const t = useTranslations();
  const { configured } = useTranslationConfig();
  const S = t.companies;
  const router = useRouter();
  const { data: session } = useSession();
  const canDelete = hasRole(session, ROLE_ADMIN);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [tags, setTags] = useState<TagDto[]>([]);

  useEffect(() => {
    if (company.tagIds.length === 0) return;
    Promise.all(company.tagIds.map((id) => getTag(id).catch(() => null)))
      .then((results) => setTags(results.filter((r): r is TagDto => r !== null)));
  }, [company.tagIds]);

  const handleDeleteAll = async () => {
    try {
      await deleteCompany(company.id, true);
      router.push("/companies");
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        console.error("Failed to delete company");
      }
    }
  };

  const handleDeleteCompanyOnly = async () => {
    try {
      await deleteCompany(company.id, false);
      router.push("/companies");
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
        <div className="flex items-center gap-4">
          {company.hasLogo ? (
            <img
              src={getCompanyLogoUrl(company.id)}
              alt={company.name}
              className="h-24 w-24 object-contain"
            />
          ) : (
            <Building2 className="h-24 w-24 text-oe-gray-mid" />
          )}
          <div className="flex flex-col">
            <h1 className="font-heading text-2xl font-bold text-oe-dark">{company.name}</h1>
            <div className="h-6">
              {company.brevo && (
                <span className="inline-block rounded border border-oe-gray-light bg-oe-gray-light/30 px-2 py-0.5 text-xs text-oe-gray-mid">
                  Brevo
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/contacts?companyId=${company.id}`}>
              <Users className="mr-2 h-4 w-4" />
              {S.detail.showEmployees} ({company.contactCount})
            </Link>
          </Button>
          <Button asChild variant="outline">
            <Link href={`/companies/${company.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.detail.edit}
            </Link>
          </Button>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>
                <Button
                  variant="outline"
                  className="text-oe-red border-oe-red hover:bg-oe-red-lighter"
                  disabled={!canDelete}
                  onClick={() => { setDeleteError(null); setDeleteOpen(true); }}
                >
                  <Trash2 className="mr-2 h-4 w-4" />
                  {S.detail.delete}
                </Button>
              </span>
            </TooltipTrigger>
            {!canDelete && (
              <TooltipContent>{t.errors.roleRequired.admin}</TooltipContent>
            )}
          </Tooltip>
        </div>
      </div>

      <Card className="border-oe-gray-light">
        <CardHeader>
          <CardTitle className="font-heading text-lg text-oe-dark">{S.detail.title}</CardTitle>
        </CardHeader>
        <CardContent>
          <dl className="grid gap-4 sm:grid-cols-2">
            <DetailField label={S.form.name} value={company.name} />
            <DetailField label={S.detail.email} value={company.email} copyable mailable />
            <DetailField label={S.detail.phone} value={company.phoneNumber} copyable callable />
            <DetailField label={S.detail.website} value={company.website} copyable linkable />
            {(() => {
              const lines: string[] = [];
              if (company.street) {
                lines.push(company.houseNumber ? `${company.street} ${company.houseNumber}` : company.street);
              }
              const line2Parts = [company.zipCode, company.city].filter(Boolean);
              if (line2Parts.length > 0) {
                lines.push(line2Parts.join(" "));
              }
              if (company.country) {
                lines.push(company.country);
              }
              const addressText = lines.length > 0 ? lines.join("\n") : null;
              return (
                <DetailField label={S.detail.address} value={addressText} copyable multiline />
              );
            })()}
          </dl>
        </CardContent>
      </Card>

      {(company.bankName || company.bic || company.iban || company.vatId) && (
        <Card className="border-oe-gray-light mt-4">
          <CardHeader>
            <CardTitle className="font-heading text-lg text-oe-dark">{S.detail.finance.title}</CardTitle>
          </CardHeader>
          <CardContent>
            <dl className="grid gap-4 sm:grid-cols-2">
              <DetailField label={S.detail.finance.bankName} value={company.bankName} copyable />
              <DetailField label={S.detail.finance.bic} value={company.bic} copyable />
              <DetailField label={S.detail.finance.iban} value={company.iban} copyable />
              <DetailField label={S.detail.finance.vatId} value={company.vatId} copyable />
            </dl>
          </CardContent>
        </Card>
      )}

      <TagChips tags={tags} label={t.tags.label} />

      {company.description && (
        <div className="mt-4">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-medium text-oe-gray-mid">{S.detail.description}</h3>
            <TranslateButton
              text={company.description}
              size="sm"
              configured={configured}
              onTranslate={(text, lang) => translateText(text, lang as "de" | "en")}
              translations={{
                button: t.translation.translate,
                dialog: {
                  title: t.translation.title,
                  loading: t.translation.loading,
                  error: t.translation.error,
                  copy: t.translation.copy,
                  copied: t.translation.copied,
                  close: t.translation.close,
                },
              }}
            />
          </div>
          <div className="mt-1 text-sm">
            <MarkdownView content={company.description} />
          </div>
        </div>
      )}

      <Separator className="my-8" />

      <CompanyComments companyId={company.id} totalCount={company.commentCount} />

      <CompanyDeleteDialog
        open={deleteOpen}
        onOpenChange={(open) => { setDeleteOpen(open); if (!open) setDeleteError(null); }}
        companyName={company.name}
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
    </div>
  );
}
