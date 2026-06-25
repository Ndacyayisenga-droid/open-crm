"use client";

import { useMemo, useRef, useState } from "react";
import { FileText, Download } from "lucide-react";
import {
  Button,
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@open-elements/ui";
import { useTranslations } from "@/lib/i18n";
import { commitContactImport, ForbiddenError, previewContactImport } from "@/lib/api";
import type {
  ContactImportPreviewResponse,
  ContactImportResult,
  ContactImportTarget,
} from "@/lib/types";

const IMPORT_TARGETS: readonly ContactImportTarget[] = [
  "TITLE",
  "FIRST_NAME",
  "LAST_NAME",
  "EMAIL",
  "POSITION",
  "PHONE_NUMBER",
  "LINKEDIN_URL",
  "WEBSITE_URL",
];

const FIELD_TO_TARGET = {
  title: "TITLE",
  firstName: "FIRST_NAME",
  lastName: "LAST_NAME",
  email: "EMAIL",
  position: "POSITION",
  phoneNumber: "PHONE_NUMBER",
  linkedInUrl: "LINKEDIN_URL",
  websiteUrl: "WEBSITE_URL",
} as const satisfies Record<string, ContactImportTarget>;

const PREVIEW_FIELDS = [
  "firstName",
  "lastName",
  "email",
  "title",
  "position",
  "phoneNumber",
  "linkedInUrl",
  "websiteUrl",
] as const;

type Step = "upload" | "mapping" | "preview" | "result";

interface CsvImportDialogProps {
  readonly open: boolean;
  readonly onOpenChange: (open: boolean) => void;
  readonly onComplete: () => void;
}

export function CsvImportDialog({ open, onOpenChange, onComplete }: CsvImportDialogProps) {
  const t = useTranslations();
  const I = t.csvImport;
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [step, setStep] = useState<Step>("upload");
  const [file, setFile] = useState<File | null>(null);
  const [encoding, setEncoding] = useState("UTF-8");
  const [hasHeader, setHasHeader] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [previewData, setPreviewData] = useState<ContactImportPreviewResponse | null>(null);
  const [mapping, setMapping] = useState<Record<string, string>>({});
  const [mappedPreview, setMappedPreview] = useState<ContactImportPreviewResponse | null>(null);
  const [result, setResult] = useState<ContactImportResult | null>(null);

  const usedTargets = useMemo(
    () => new Set(Object.values(mapping).filter((value) => value !== "IGNORE")),
    [mapping],
  );

  const mappingValid =
    usedTargets.has("FIRST_NAME") &&
    usedTargets.has("LAST_NAME") &&
    usedTargets.size === Object.values(mapping).filter((v) => v !== "IGNORE").length;

  const reset = () => {
    setStep("upload");
    setFile(null);
    setEncoding("UTF-8");
    setHasHeader(true);
    setLoading(false);
    setError(null);
    setPreviewData(null);
    setMapping({});
    setMappedPreview(null);
    setResult(null);
  };

  const handleOpenChange = (next: boolean) => {
    if (!next) {
      if (result && result.createdCount > 0) {
        onComplete();
      }
      reset();
    }
    onOpenChange(next);
  };

  const buildRequest = (mappingValue: Record<string, string> | null) => ({
    encoding,
    hasHeader,
    mapping: mappingValue,
  });

  const handleUploadNext = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const response = await previewContactImport(file, buildRequest(null));
      setPreviewData(response);
      const initialMapping: Record<string, string> = {};
      for (const column of response.columns) {
        initialMapping[column] = "IGNORE";
      }
      setMapping(initialMapping);
      setStep("mapping");
    } catch (error) {
      if (error instanceof ForbiddenError) {
        setError(I.errors.forbidden);
      } else {
        setError(I.errors.generic);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleMappingNext = async () => {
    if (!file || !mappingValid) return;
    setLoading(true);
    setError(null);
    try {
      const activeMapping = Object.fromEntries(
        Object.entries(mapping).filter(([, target]) => target !== "IGNORE"),
      );
      const response = await previewContactImport(file, buildRequest(activeMapping));
      setMappedPreview(response);
      setStep("preview");
    } catch (error) {
      if (error instanceof ForbiddenError) {
        setError(I.errors.forbidden);
      } else {
        setError(I.errors.generic);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!file || !mappingValid) return;
    setLoading(true);
    setError(null);
    try {
      const activeMapping = Object.fromEntries(
        Object.entries(mapping).filter(([, target]) => target !== "IGNORE"),
      );
      const response = await commitContactImport(file, buildRequest(activeMapping));
      setResult(response);
      setStep("result");
    } catch (error) {
      if (error instanceof ForbiddenError) {
        setError(I.errors.forbidden);
      } else {
        setError(I.errors.generic);
      }
    } finally {
      setLoading(false);
    }
  };

  const downloadFailureCsv = () => {
    if (!result || !previewData) return;
    const delimiter = previewData.delimiter;
    const headers = [...previewData.columns, "_error_field", "_error_reason"];
    const lines = [headers.join(delimiter)];
    for (const failure of result.failures) {
      const cells = previewData.columns.map((column) => escapeCsvCell(failure.cells[column] ?? "", delimiter));
      lines.push(
        [...cells, failure.field ?? "", failure.reason].map((cell) => escapeCsvCell(cell, delimiter)).join(delimiter),
      );
    }
    const blob = new Blob([lines.join("\n")], {
      type: encoding === "Windows-1252" ? "text/csv;charset=windows-1252" : "text/csv;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "import-failures.csv";
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const fieldLabel = (field: (typeof PREVIEW_FIELDS)[number]) =>
    I.mapping.targets[FIELD_TO_TARGET[field]];

  const stepDescription = (() => {
    switch (step) {
      case "upload":
        return I.upload.description;
      case "mapping":
        return I.mapping.description;
      case "preview":
        return I.preview.description;
      case "result":
        return result
          ? I.result.summary
              .replace("{created}", String(result.createdCount))
              .replace("{failed}", String(result.failedCount))
          : "";
    }
  })();

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className={`max-h-[90vh] overflow-y-auto ${step === "upload" ? "max-w-lg" : "max-w-3xl"}`}
      >
        <DialogHeader className="space-y-2 text-left">
          <DialogTitle className="font-heading text-xl text-oe-dark">{I.title}</DialogTitle>
          {stepDescription && (
            <p className="text-sm font-normal text-oe-gray-mid leading-relaxed">{stepDescription}</p>
          )}
        </DialogHeader>

        {error && <p className="text-sm text-red-600">{error}</p>}

        {step === "upload" && (
          <div className="space-y-5 pt-1">
            <div className="flex w-full overflow-hidden rounded-md border border-oe-gray-light bg-white">
              <label
                htmlFor="csv-import-file"
                className="cursor-pointer shrink-0 border-r border-oe-gray-light bg-oe-gray-lightest px-4 py-2.5 text-sm text-oe-dark hover:bg-oe-gray-light transition-colors"
              >
                {I.upload.chooseFile}
              </label>
              <input
                ref={fileInputRef}
                id="csv-import-file"
                type="file"
                accept=".csv,text/csv"
                className="sr-only"
                onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
              <span className="flex min-w-0 flex-1 items-center px-3 py-2.5 text-sm text-oe-gray-mid truncate">
                {file?.name ?? I.upload.noFileChosen}
              </span>
            </div>

            <div className="flex flex-wrap items-end justify-between gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="csv-import-encoding" className="text-sm text-oe-dark">
                  {I.upload.encoding}
                </Label>
                <Select value={encoding} onValueChange={setEncoding}>
                  <SelectTrigger id="csv-import-encoding" className="w-[140px]">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="UTF-8">{I.upload.encodingUtf8}</SelectItem>
                    <SelectItem value="Windows-1252">{I.upload.encodingWindows1252}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <label className="flex cursor-pointer items-center gap-2 pb-2 text-sm text-oe-dark">
                <input
                  type="checkbox"
                  checked={hasHeader}
                  onChange={(event) => setHasHeader(event.target.checked)}
                  className="h-4 w-4 rounded border-oe-gray-light text-oe-green focus:ring-oe-green accent-oe-green"
                />
                {I.upload.hasHeader}
              </label>
            </div>
          </div>
        )}

        {step === "mapping" && previewData && (
          <div className="space-y-3">
            <p className="text-sm text-oe-gray-mid">
              {I.upload.rowsDetected.replace("{count}", String(previewData.totalRows))}
            </p>
            {!mappingValid && <p className="text-sm text-amber-700">{I.mapping.requiredHint}</p>}
            <div className="grid grid-cols-2 gap-x-4 gap-y-2 max-h-[320px] overflow-y-auto pr-1">
              <span className="text-xs font-medium uppercase tracking-wide text-oe-gray-mid pb-1">
                {I.mapping.column}
              </span>
              <span className="text-xs font-medium uppercase tracking-wide text-oe-gray-mid pb-1">
                {I.mapping.target}
              </span>
              {previewData.columns.map((column) => (
                <div key={column} className="contents">
                  <span className="text-sm truncate self-center py-1" title={column}>
                    {column}
                  </span>
                  <Select
                    value={mapping[column] ?? "IGNORE"}
                    onValueChange={(value) => setMapping((prev) => ({ ...prev, [column]: value }))}
                  >
                    <SelectTrigger className="h-9">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="IGNORE">{I.mapping.ignore}</SelectItem>
                      {IMPORT_TARGETS.map((target) => (
                        <SelectItem
                          key={target}
                          value={target}
                          disabled={usedTargets.has(target) && mapping[column] !== target}
                        >
                          {I.mapping.targets[target]}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              ))}
            </div>
          </div>
        )}

        {step === "preview" && mappedPreview?.sampleContacts && (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{I.preview.row}</TableHead>
                  {PREVIEW_FIELDS.map((field) => (
                    <TableHead key={field}>{fieldLabel(field)}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {mappedPreview.sampleContacts.map((sample) => (
                  <TableRow key={sample.row}>
                    <TableCell>{sample.row}</TableCell>
                    {PREVIEW_FIELDS.map((field) => {
                      const value = sample.contact[field];
                      const fieldError = sample.errors.find((entry) => entry.field === field);
                      return (
                        <TableCell
                          key={field}
                          className={fieldError ? "bg-red-50 text-red-700" : undefined}
                          title={fieldError ? I.errors[fieldError.reason as keyof typeof I.errors] ?? fieldError.reason : undefined}
                        >
                          {value ?? "—"}
                        </TableCell>
                      );
                    })}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}

        {step === "result" && result && (
          <div className="space-y-4">
            {result.failures.length > 0 && (
              <>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{I.result.columns.row}</TableHead>
                      <TableHead>{I.result.columns.field}</TableHead>
                      <TableHead>{I.result.columns.reason}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {result.failures.map((failure) => (
                      <TableRow key={failure.row}>
                        <TableCell>{failure.row}</TableCell>
                        <TableCell>{failure.field ?? "—"}</TableCell>
                        <TableCell>
                          {I.errors[failure.reason as keyof typeof I.errors] ?? failure.reason}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
                <Button variant="outline" onClick={downloadFailureCsv}>
                  <Download className="mr-2 h-4 w-4" />
                  {I.result.downloadFailures}
                </Button>
              </>
            )}
          </div>
        )}

        <DialogFooter className="mt-6 flex flex-row justify-end gap-2 sm:justify-end">
          {step !== "result" ? (
            <>
              <Button variant="outline" onClick={() => handleOpenChange(false)} disabled={loading}>
                {I.cancel}
              </Button>
              {step !== "upload" && (
                <Button
                  variant="outline"
                  onClick={() => setStep(step === "preview" ? "mapping" : "upload")}
                  disabled={loading}
                >
                  {I.back}
                </Button>
              )}
              {step === "upload" && (
                <Button type="button" onClick={handleUploadNext} disabled={!file || loading}>
                  <FileText className="mr-2 h-4 w-4" />
                  {I.next}
                </Button>
              )}
              {step === "mapping" && (
                <Button onClick={handleMappingNext} disabled={!mappingValid || loading}>
                  <FileText className="mr-2 h-4 w-4" />
                  {I.next}
                </Button>
              )}
              {step === "preview" && (
                <Button onClick={handleCommit} disabled={loading}>
                  <FileText className="mr-2 h-4 w-4" />
                  {I.import}
                </Button>
              )}
            </>
          ) : (
            <Button onClick={() => handleOpenChange(false)}>{I.close}</Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function escapeCsvCell(value: string, delimiter: string): string {
  if (value.includes(delimiter) || value.includes('"') || value.includes("\n")) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}
