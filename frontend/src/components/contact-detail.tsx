"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "next-auth/react";
import Link from "next/link";
import { Contact, Mail, Pencil, Trash2, User } from "lucide-react";
import { Button, Card, CardContent, CardHeader, CardTitle, DeleteConfirmDialog, DetailField, Separator, Tooltip, TooltipContent, TooltipTrigger, TagChips, MarkdownView, TranslateButton } from "@open-elements/ui";
import type { TagDto } from "@open-elements/ui";
import { useTranslations, useLanguage } from "@/lib/i18n";
import { ContactComments } from "@/components/contact-comments";
import { useTranslationConfig } from "@/lib/use-translation-config";
import { deleteContact, ForbiddenError, getContactPhotoUrl, getContactVCardUrl, getTag, translateText } from "@/lib/api";
import type { ContactDto } from "@/lib/types";
import { hasAppAdmin } from "@/lib/roles";

function genderLabel(gender: string | null, t: ReturnType<typeof useTranslations>): string | null {
  if (!gender) return null;
  const S = t.contacts.form;
  switch (gender) {
    case "MALE":
      return S.male;
    case "FEMALE":
      return S.female;
    case "DIVERSE":
      return S.diverse;
    default:
      return gender;
  }
}

function languageLabel(language: string | null, t: ReturnType<typeof useTranslations>): string {
  const S = t.contacts.detail;
  switch (language) {
    case "DE":
      return S.languageDE;
    case "EN":
      return S.languageEN;
    default:
      return S.languageUnknown;
  }
}

function formatBirthday(dateStr: string | null, lang: string): string | null {
  if (!dateStr) return null;
  const [year, month, day] = dateStr.split("-");
  if (lang === "de") return `${day}.${month}.${year}`;
  return `${month}/${day}/${year}`;
}

interface ContactDetailProps {
  readonly contact: ContactDto;
}

export function ContactDetail({ contact }: ContactDetailProps) {
  const t = useTranslations();
  const S = t.contacts;
  const { language } = useLanguage();
  const { configured } = useTranslationConfig();
  const router = useRouter();
  const { data: session } = useSession();
  const canDelete = hasAppAdmin(session);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [tags, setTags] = useState<TagDto[]>([]);

  useEffect(() => {
    if (contact.tagIds.length === 0) return;
    Promise.all(contact.tagIds.map((id) => getTag(id).catch(() => null)))
      .then((results) => setTags(results.filter((r): r is TagDto => r !== null)));
  }, [contact.tagIds]);

  const handleDelete = async () => {
    try {
      await deleteContact(contact.id);
      router.push("/contacts");
    } catch (e) {
      if (e instanceof ForbiddenError) {
        setDeleteError(t.errors.forbidden.deleteNoPermission);
      } else {
        setDeleteError(S.form.errorGeneric);
      }
    }
  };

  const fullName = `${contact.title ? contact.title + " " : ""}${contact.firstName} ${contact.lastName}`;

  return (
    <div>
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-center gap-4">
          {contact.hasPhoto ? (
            <img
              src={getContactPhotoUrl(contact.id)}
              alt={fullName}
              className="h-24 w-24 rounded-full object-cover"
            />
          ) : (
            <User className="h-24 w-24 text-oe-gray-mid" />
          )}
          <div className="flex flex-col">
            <h1 className="font-heading text-2xl font-bold text-oe-dark">{fullName}</h1>
            <div className="h-6">
              {contact.brevo && (
                <span className="inline-block rounded border border-oe-gray-light bg-oe-gray-light/30 px-2 py-0.5 text-xs text-oe-gray-mid">
                  Brevo
                </span>
              )}
              {contact.receivesNewsletter && (
                <span className="inline-flex items-center gap-1 rounded border border-oe-green/30 bg-oe-green/10 px-2 py-0.5 text-xs text-oe-green">
                  <Mail className="h-3 w-3" />
                  Newsletter
                </span>
              )}
            </div>
          </div>
        </div>
        <div className="flex gap-2">
          <Button asChild variant="outline">
            <Link href={`/contacts/${contact.id}/edit`}>
              <Pencil className="mr-2 h-4 w-4" />
              {S.detail.edit}
            </Link>
          </Button>
          <Button
            variant="outline"
            onClick={() => window.open(getContactVCardUrl(contact.id), "_blank")}
          >
            <Contact className="mr-2 h-4 w-4" />
            {t.vcardExport.single}
          </Button>
          <Tooltip>
            <TooltipTrigger asChild>
              <span>
                <Button
                  variant="outline"
                  className="text-oe-red border-oe-red hover:bg-oe-red-lighter"
                  disabled={!canDelete}
                  onClick={() => {
                    setDeleteError(null);
                    setDeleteOpen(true);
                  }}
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
            <DetailField label={S.detail.email} value={contact.email} copyable mailable />
            <DetailField label={S.detail.position} value={contact.position} />
            <DetailField label={S.detail.gender} value={genderLabel(contact.gender, t)} />
            <DetailField label={S.detail.phone} value={contact.phoneNumber} copyable callable />
            {contact.socialLinks && contact.socialLinks.length > 0 && (() => {
              const displayOrder = ["LINKEDIN", "GITHUB", "MASTODON", "BLUESKY", "DISCORD", "WEBSITE", "X", "YOUTUBE"];
              const networkNames: Record<string, string> = {
                GITHUB: S.form.networkGithub,
                LINKEDIN: S.form.networkLinkedin,
                X: S.form.networkX,
                MASTODON: S.form.networkMastodon,
                BLUESKY: S.form.networkBluesky,
                DISCORD: S.form.networkDiscord,
                YOUTUBE: S.form.networkYoutube,
                WEBSITE: S.form.networkWebsite,
              };
              const grouped = displayOrder
                .map(network => ({
                  network,
                  links: contact.socialLinks.filter(l => l.networkType === network),
                }))
                .filter(g => g.links.length > 0);

              return grouped.flatMap(g =>
                g.links.map((l, i) => (
                  <DetailField
                    key={`${g.network}-${i}`}
                    label={i === 0 ? (networkNames[g.network] ?? g.network) : ""}
                    value={l.url}
                    copyable
                    linkable
                  >
                    {l.value}
                  </DetailField>
                ))
              );
            })()}
            <DetailField label={S.detail.birthday} value={formatBirthday(contact.birthday, language)} />
            <DetailField label={S.detail.language} value={languageLabel(contact.language, t)} />
            <div>
              <dt className="text-sm font-medium text-oe-gray-mid">{S.detail.company}</dt>
              <dd className="mt-1 text-sm text-oe-black">
                {contact.companyId && contact.companyName ? (
                  <Link
                    href={`/companies/${contact.companyId}`}
                    className="text-oe-green hover:text-oe-green-dark underline"
                  >
                    {contact.companyName}
                    <span className="ml-1 text-xs no-underline">({S.detail.showCompany})</span>
                  </Link>
                ) : (
                  "—"
                )}
              </dd>
            </div>
          </dl>
        </CardContent>
      </Card>

      <TagChips tags={tags} label={t.tags.label} />

      {contact.description && (
        <div className="mt-4">
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-medium text-oe-gray-mid">{S.detail.description}</h3>
            <TranslateButton
              text={contact.description}
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
            <MarkdownView content={contact.description} />
          </div>
        </div>
      )}

      <Separator className="my-8" />

      <ContactComments contactId={contact.id} totalCount={contact.commentCount} />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        title={S.deleteDialog.title}
        description={S.deleteDialog.description.replace("{name}", fullName)}
        confirmLabel={S.deleteDialog.confirm}
        cancelLabel={S.deleteDialog.cancel}
        onConfirm={handleDelete}
        error={deleteError}
      />
    </div>
  );
}
