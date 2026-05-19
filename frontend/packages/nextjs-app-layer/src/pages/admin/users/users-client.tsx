"use client";

import { useCallback, useEffect, useState } from "react";
import { AlertCircle, User as UserIcon, Users as UsersIcon } from "lucide-react";
import {
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  TablePagination,
} from "@open-elements/ui";
import { useAppLayerTranslations } from "../../../translations/provider";
import { useApiClient } from "../../../hooks/api-client";
import type { Page, UserDto } from "../../../api/types";

export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100, 200] as const;
export const DEFAULT_PAGE_SIZE = 20;
export const PAGE_SIZE_STORAGE_KEY = "pageSize.users";

function readStoredPageSize(): number {
  if (typeof window === "undefined") return DEFAULT_PAGE_SIZE;
  const stored = localStorage.getItem(PAGE_SIZE_STORAGE_KEY);
  const parsed = Number(stored);
  if ((PAGE_SIZE_OPTIONS as readonly number[]).includes(parsed)) return parsed;
  return DEFAULT_PAGE_SIZE;
}

export function UsersClient() {
  const t = useAppLayerTranslations();
  const api = useApiClient();
  const [data, setData] = useState<Page<UserDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState<number>(() => readStoredPageSize());

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.getUsers({ page, size: pageSize });
      setData(result);
    } catch (err: unknown) {
      console.error("Failed to load users", err);
      setError(t.users.loadError);
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [api, page, pageSize, t.users.loadError]);

  useEffect(() => {
    fetchUsers();
  }, [fetchUsers]);

  const totalElements = data?.page.totalElements ?? 0;
  const totalPages = data?.page.totalPages ?? 0;

  return (
    <div>
      <h1 className="mb-6 font-heading text-2xl font-bold text-oe-dark">{t.users.title}</h1>

      {loading ? (
        <div className="space-y-3" data-testid="users-loading">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-12 w-full" />
          ))}
        </div>
      ) : error ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="users-error"
          role="alert"
        >
          <AlertCircle className="mb-4 h-12 w-12 text-oe-red/70" />
          <p className="text-oe-red">{error}</p>
        </div>
      ) : !data || data.content.length === 0 ? (
        <div
          className="flex flex-col items-center justify-center py-16 text-center"
          data-testid="users-empty"
        >
          <UsersIcon className="mb-4 h-12 w-12 text-oe-gray/50" />
          <p className="text-oe-gray">{t.users.empty}</p>
        </div>
      ) : (
        <>
          <div className="overflow-hidden rounded-lg border border-oe-gray-light">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">{t.users.columns.avatar}</TableHead>
                  <TableHead>{t.users.columns.name}</TableHead>
                  <TableHead>{t.users.columns.email}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.content.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>
                      {user.avatarUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={user.avatarUrl}
                          alt=""
                          className="h-8 w-8 rounded-full object-cover"
                        />
                      ) : (
                        <div
                          className="flex h-8 w-8 items-center justify-center rounded-full bg-oe-gray-lightest text-oe-gray"
                          data-testid="user-avatar-fallback"
                          aria-hidden="true"
                        >
                          <UserIcon className="h-4 w-4" />
                        </div>
                      )}
                    </TableCell>
                    <TableCell className="font-medium text-oe-dark">{user.name}</TableCell>
                    <TableCell className="text-oe-gray">{user.email}</TableCell>
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
            translations={t.users.pagination}
            onPageChange={setPage}
            onPageSizeChange={setPageSize}
          />
        </>
      )}
    </div>
  );
}
