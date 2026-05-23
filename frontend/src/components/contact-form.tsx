"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { User, Trash2, Upload } from "lucide-react";
import { Button, Input, TagMultiSelect, Label, Card, CardContent, CardHeader, CardTitle, Select, SelectContent, SelectItem, SelectTrigger, SelectValue, MarkdownEditor } from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { createContact, updateContact, getCompaniesForSelect, uploadContactPhoto, deleteContactPhoto, getContactPhotoUrl, getTags } from "@/lib/api";
import type { ContactDto, ContactCreateDto, CompanyDto } from "@/lib/types";

const MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
const ALLOWED_CONTACT_PHOTO_TYPES = ["image/jpeg", "image/png"] as const;
const CONTACT_PHOTO_ACCEPT = ALLOWED_CONTACT_PHOTO_TYPES.join(",");

interface ContactFormProps {
  readonly contact?: ContactDto;
}

export function ContactForm({ contact }: ContactFormProps) {
  const t = useTranslations();
  const S = t.contacts.form;
  const router = useRouter();
  const isEdit = !!contact;
  const isBrevoEdit = isEdit && (contact?.brevo === true);

  const [title, setTitle] = useState(contact?.title ?? "");
  const [firstName, setFirstName] = useState(contact?.firstName ?? "");
  const [lastName, setLastName] = useState(contact?.lastName ?? "");
  const [email, setEmail] = useState(contact?.email ?? "");
  const [position, setPosition] = useState(contact?.position ?? "");
  const [gender, setGender] = useState(contact?.gender ?? "");
  const [socialLinks, setSocialLinks] = useState<{networkType: string; value: string}[]>(
    contact?.socialLinks?.map(l => ({networkType: l.networkType, value: l.value})) ?? []
  );
  const [phoneNumber, setPhoneNumber] = useState(contact?.phoneNumber ?? "");
  const [companyId, setCompanyId] = useState(contact?.companyId ?? "");
  const [language, setLanguage] = useState(contact?.language ?? "");
  const [birthday, setBirthday] = useState(contact?.birthday ?? "");
  const [description, setDescription] = useState(contact?.description ?? "");
  const [tagIds, setTagIds] = useState<string[]>([...(contact?.tagIds ?? [])]);
  const [tagIdsChanged, setTagIdsChanged] = useState(false);

  const [companies, setCompanies] = useState<CompanyDto[]>([]);

  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [filePreview, setFilePreview] = useState<string | null>(null);
  const [hasExistingPhoto, setHasExistingPhoto] = useState(contact?.hasPhoto ?? false);
  const [removeExistingPhoto, setRemoveExistingPhoto] = useState(false);
  const [imageError, setImageError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setImageError(null);
    const file = e.target.files?.[0];
    if (!file) return;

    if (!ALLOWED_CONTACT_PHOTO_TYPES.includes(file.type as (typeof ALLOWED_CONTACT_PHOTO_TYPES)[number])) {
      setImageError(S.imageInvalidFormat);
      return;
    }
    if (file.size > MAX_FILE_SIZE) {
      setImageError(S.imageTooLarge);
      return;
    }

    setSelectedFile(file);
    setFilePreview(URL.createObjectURL(file));
    setRemoveExistingPhoto(false);
  };

  const handleRemovePhoto = () => {
    setSelectedFile(null);
    setFilePreview(null);
    setRemoveExistingPhoto(true);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const [firstNameError, setFirstNameError] = useState<string | null>(null);
  const [lastNameError, setLastNameError] = useState<string | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCompaniesForSelect()
      .then(setCompanies)
      .catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFirstNameError(null);
    setLastNameError(null);
    setApiError(null);

    let hasError = false;

    if (!firstName.trim()) {
      setFirstNameError(S.firstNameRequired);
      hasError = true;
    }
    if (!lastName.trim()) {
      setLastNameError(S.lastNameRequired);
      hasError = true;
    }

    if (hasError) return;

    const data: ContactCreateDto = {
      title: title.trim() || null,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim() || null,
      position: position.trim() || null,
      gender: gender && gender !== "none" ? (gender as "MALE" | "FEMALE" | "DIVERSE") : null,
      socialLinks: socialLinks.filter(l => l.value.trim()).map(l => ({networkType: l.networkType, value: l.value.trim()})),
      phoneNumber: phoneNumber.trim() || null,
      companyId: companyId && companyId !== "none" ? companyId : null,
      language: language && language !== "unknown" ? (language as "DE" | "EN") : null,
      birthday: birthday || null,
      description: description.trim() || null,
      ...(tagIdsChanged ? { tagIds } : {}),
    };

    setSubmitting(true);
    try {
      let result: ContactDto;
      if (isEdit) {
        result = await updateContact(contact.id, data);
      } else {
        result = await createContact(data);
      }

      // Handle photo upload/delete after entity save
      if (removeExistingPhoto && !selectedFile) {
        await deleteContactPhoto(result.id);
      }
      if (selectedFile) {
        await uploadContactPhoto(result.id, selectedFile);
      }

      router.push(`/contacts/${result.id}`);
    } catch {
      setApiError(S.errorGeneric);
    } finally {
      setSubmitting(false);
    }
  };

  const cancelHref = isEdit ? `/contacts/${contact.id}` : "/contacts";

  return (
    <Card className="mx-auto max-w-2xl border-oe-gray-light">
      <CardHeader>
        <CardTitle className="font-heading text-xl text-oe-dark">
          {isEdit ? S.editTitle : S.createTitle}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 grid-cols-[1fr_2fr_2fr]">
            <div>
              <Label htmlFor="title">{S.title}</Label>
              <Input
                id="title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder={S.titlePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="firstName">{S.firstName} *</Label>
              <Input
                id="firstName"
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder={S.firstNamePlaceholder}
                className={firstNameError ? "border-oe-red" : ""}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
              {firstNameError && <p className="mt-1 text-sm text-oe-red">{firstNameError}</p>}
            </div>
            <div>
              <Label htmlFor="lastName">{S.lastName} *</Label>
              <Input
                id="lastName"
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder={S.lastNamePlaceholder}
                className={lastNameError ? "border-oe-red" : ""}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
              {lastNameError && <p className="mt-1 text-sm text-oe-red">{lastNameError}</p>}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="email">{S.email}</Label>
              <Input
                id="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder={S.emailPlaceholder}
                disabled={isBrevoEdit}
              />
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
            </div>
            <div>
              <Label htmlFor="position">{S.position}</Label>
              <Input
                id="position"
                value={position}
                onChange={(e) => setPosition(e.target.value)}
                placeholder={S.positionPlaceholder}
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="gender">{S.gender}</Label>
              <Select value={gender || "none"} onValueChange={setGender}>
                <SelectTrigger id="gender">
                  <SelectValue placeholder={S.notSpecified} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{S.notSpecified}</SelectItem>
                  <SelectItem value="MALE">{S.male}</SelectItem>
                  <SelectItem value="FEMALE">{S.female}</SelectItem>
                  <SelectItem value="DIVERSE">{S.diverse}</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label htmlFor="language">{S.language}</Label>
              <Select value={language || "unknown"} onValueChange={setLanguage} disabled={isBrevoEdit}>
                <SelectTrigger id="language">
                  <SelectValue placeholder={S.language} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="unknown">{S.languageUnknown}</SelectItem>
                  <SelectItem value="DE">DE</SelectItem>
                  <SelectItem value="EN">EN</SelectItem>
                </SelectContent>
              </Select>
              {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="phone">{S.phone}</Label>
              <Input
                id="phone"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                placeholder={S.phonePlaceholder}
              />
            </div>
            <div>
              <Label htmlFor="birthday">{S.birthday}</Label>
              <Input
                id="birthday"
                type="date"
                value={birthday}
                onChange={(e) => setBirthday(e.target.value)}
              />
            </div>
          </div>

          {/* Social Links */}
          <fieldset className="space-y-2" disabled={isBrevoEdit}>
            <Label>{S.socialLinks}</Label>
            {socialLinks.map((link, index) => {
              const placeholders: Record<string, string> = {
                GITHUB: S.placeholderGithub,
                LINKEDIN: S.placeholderLinkedin,
                X: S.placeholderX,
                MASTODON: S.placeholderMastodon,
                BLUESKY: S.placeholderBluesky,
                DISCORD: S.placeholderDiscord,
                YOUTUBE: S.placeholderYoutube,
                WEBSITE: S.placeholderWebsite,
              };
              return (
                <div key={index} className="flex gap-2 items-center">
                  <Select
                    value={link.networkType}
                    onValueChange={(val) => {
                      const updated = [...socialLinks];
                      updated[index] = { ...updated[index], networkType: val };
                      setSocialLinks(updated);
                    }}
                  >
                    <SelectTrigger className="w-40 shrink-0">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="GITHUB">{S.networkGithub}</SelectItem>
                      <SelectItem value="LINKEDIN">{S.networkLinkedin}</SelectItem>
                      <SelectItem value="X">{S.networkX}</SelectItem>
                      <SelectItem value="MASTODON">{S.networkMastodon}</SelectItem>
                      <SelectItem value="BLUESKY">{S.networkBluesky}</SelectItem>
                      <SelectItem value="DISCORD">{S.networkDiscord}</SelectItem>
                      <SelectItem value="YOUTUBE">{S.networkYoutube}</SelectItem>
                      <SelectItem value="WEBSITE">{S.networkWebsite}</SelectItem>
                    </SelectContent>
                  </Select>
                  <Input
                    value={link.value}
                    onChange={(e) => {
                      const updated = [...socialLinks];
                      updated[index] = { ...updated[index], value: e.target.value };
                      setSocialLinks(updated);
                    }}
                    placeholder={placeholders[link.networkType] ?? ""}
                    className="flex-1"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="shrink-0 text-oe-red border-oe-red hover:bg-oe-red-lighter"
                    onClick={() => {
                      setSocialLinks(socialLinks.filter((_, i) => i !== index));
                    }}
                  >
                    &times;
                  </Button>
                </div>
              );
            })}
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setSocialLinks([...socialLinks, { networkType: "LINKEDIN", value: "" }])}
            >
              + {S.addSocialLink}
            </Button>
            {isBrevoEdit && <p className="mt-1 text-xs text-oe-gray-mid">{S.managedByBrevo}</p>}
          </fieldset>

          <div>
            <Label htmlFor="company">{S.company}</Label>
            <Select value={companyId || "none"} onValueChange={setCompanyId}>
              <SelectTrigger id="company">
                <SelectValue placeholder={S.noCompany} />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="none">{S.noCompany}</SelectItem>
                {companies.map((c) => (
                  <SelectItem key={c.id} value={c.id}>
                    {c.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label>{t.tags.label}</Label>
            <TagMultiSelect
              selectedIds={tagIds}
              onChange={(ids) => { setTagIds(ids); setTagIdsChanged(true); }}
              loadTags={async () => { const result = await getTags({ size: 1000 }); return result.content.map(tag => ({ value: tag.id, label: tag.name, color: tag.color })); }}
              translations={{ placeholder: t.tags.label + "...", empty: t.tags.empty }}
            />
          </div>

          <div className="space-y-2">
            <Label>{S.description}</Label>
            <MarkdownEditor
              value={description}
              onChange={setDescription}
              placeholder={S.descriptionPlaceholder}
            />
          </div>

          {/* Photo upload */}
          <div>
            <Label>{S.photo}</Label>
            <div className="mt-2 flex items-center gap-4">
              {filePreview ? (
                <img src={filePreview} alt="Photo preview" className="h-16 w-16 rounded-full object-cover" />
              ) : hasExistingPhoto && !removeExistingPhoto ? (
                <img src={getContactPhotoUrl(contact!.id)} alt="Current photo" className="h-16 w-16 rounded-full object-cover" />
              ) : (
                <User className="h-16 w-16 text-oe-gray-mid" />
              )}
              <div className="flex flex-col gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={CONTACT_PHOTO_ACCEPT}
                  onChange={handleFileChange}
                  className="hidden"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => fileInputRef.current?.click()}
                >
                  <Upload className="mr-2 h-4 w-4" />
                  {S.uploadPhoto}
                </Button>
                {(hasExistingPhoto || selectedFile) && !removeExistingPhoto && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="text-oe-red border-oe-red"
                    onClick={handleRemovePhoto}
                  >
                    <Trash2 className="mr-2 h-4 w-4" />
                    {S.removePhoto}
                  </Button>
                )}
              </div>
            </div>
            {imageError && <p className="mt-1 text-sm text-oe-red">{imageError}</p>}
          </div>

          {apiError && <p className="text-sm text-oe-red">{apiError}</p>}

          <div className="flex gap-3 pt-4">
            <Button
              type="submit"
              disabled={submitting}
            >
              {S.save}
            </Button>
            <Button
              type="button"
              variant="outline"
              onClick={() => router.push(cancelHref)}
            >
              {S.cancel}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
